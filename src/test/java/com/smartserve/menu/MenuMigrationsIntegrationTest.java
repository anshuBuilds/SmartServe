package com.smartserve.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartserve.TestcontainersConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Runs the real Flyway migrations step by step in a throwaway schema, so each migration is
 * tested against the schema it actually runs on: the V4 menu seed on top of V3, and the V5
 * per-restaurant ownership change on top of the seeded menu.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MenuMigrationsIntegrationTest {

    private static final String SEED_SCRIPT = "db/migration/V4__seed_indian_menu.sql";
    private static final int SEED_CATEGORIES = 10;
    private static final int SEED_DISHES = 90;
    private static final String IMAGE_PREFIX = "https://smartserve.anshuyadav.me/menu/";

    @Autowired
    private PostgreSQLContainer<?> postgres;

    @Autowired
    private JdbcTemplate adminJdbc;

    private String schema;
    private DataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void freshSchema() {
        schema = "menu_migration_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        adminJdbc.execute("CREATE SCHEMA " + schema);
        String url = postgres.getJdbcUrl();
        dataSource = new DriverManagerDataSource(
                url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                postgres.getUsername(),
                postgres.getPassword());
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void dropSchema() {
        adminJdbc.execute("DROP SCHEMA " + schema + " CASCADE");
    }

    // V4: Indian menu seed

    @Test
    void seedsEmptyMenu() {
        migrateTo("4");

        assertThat(count("SELECT COUNT(*) FROM menu_categories")).isEqualTo(SEED_CATEGORIES);
        assertThat(count("SELECT COUNT(*) FROM menu_items")).isEqualTo(SEED_DISHES);
        assertThat(count("SELECT COUNT(*) FROM menu_items WHERE available = FALSE")).isZero();
    }

    @Test
    void seedRunTwiceChangesNothing() {
        migrateTo("4");
        List<String> before = seedSnapshot();

        runSeedAgain();

        assertThat(seedSnapshot()).isEqualTo(before);
    }

    @Test
    void seedKeepsExistingCategoriesAndDoesNotDuplicateExistingDishNames() {
        migrateTo("3");
        long mains = insertCategory(null, "main course", 2);
        long specials = insertCategory(null, "Chef Specials", 0);
        // Same dish as the seed's "Butter Chicken", different case/whitespace and category.
        long existing = insertItem("  butter CHICKEN ", specials);
        insertItem("House Thali", mains);

        migrateTo("4");
        runSeedAgain();

        assertThat(count("SELECT COUNT(*) FROM menu_categories")).isEqualTo(SEED_CATEGORIES + 1);
        assertThat(count("SELECT COUNT(*) FROM menu_items")).isEqualTo(2 + SEED_DISHES - 1);
        assertThat(jdbc.queryForList(
                "SELECT id FROM menu_items WHERE LOWER(BTRIM(name)) = 'butter chicken'", Long.class))
                .containsExactly(existing);
        // The existing category is reused and moved into the standard menu order...
        assertThat(count("SELECT display_order FROM menu_categories WHERE id = ?", mains)).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM menu_items WHERE category_id = ?", mains)).isGreaterThan(1);
        // ...while categories outside the seed keep theirs.
        assertThat(count("SELECT display_order FROM menu_categories WHERE id = ?", specials)).isZero();
    }

    @Test
    void everySeededPhotoIsShippedWithTheFrontend() {
        migrateTo("4");

        List<String> urls = jdbc.queryForList(
                "SELECT image_url FROM menu_items WHERE image_url IS NOT NULL", String.class);
        assertThat(urls).isNotEmpty();
        Path menuPhotos = Path.of("frontend", "public", "menu");
        assertThat(Files.isDirectory(menuPhotos)).isTrue();
        for (String url : urls) {
            assertThat(url).startsWith(IMAGE_PREFIX);
            assertThat(menuPhotos.resolve(url.substring(IMAGE_PREFIX.length()))).isRegularFile();
        }
    }

    // V5: menus per restaurant

    @Test
    void existingMenuGoesToTheFirstRestaurantCreated() {
        migrateTo("4");
        long first = insertRestaurant("Original Restaurant");
        insertRestaurant("Second Restaurant");

        migrateTo("5");

        assertThat(count("SELECT COUNT(*) FROM restaurants")).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT DISTINCT restaurant_id FROM menu_categories", Long.class))
                .containsExactly(first);
    }

    @Test
    void freshInstallGetsARestaurantToOwnTheSeededMenu() {
        migrateTo("5");

        assertThat(jdbc.queryForList("SELECT name FROM restaurants", String.class))
                .containsExactly("SmartServe Restaurant");
        assertThat(count("SELECT COUNT(*) FROM menu_categories WHERE restaurant_id IS NULL")).isZero();
        assertThat(count("SELECT COUNT(*) FROM menu_categories")).isEqualTo(SEED_CATEGORIES);
    }

    @Test
    void categoryNamesAreUniquePerRestaurantOnly() {
        migrateTo("5");
        long owner = count("SELECT MIN(id) FROM restaurants");
        long other = insertRestaurant("Coastal Kitchen");

        // The same name is fine in another restaurant...
        insertCategory(other, "Soups", 1);
        // ...but not twice in one restaurant, ignoring case and surrounding spaces.
        assertThatThrownBy(() -> insertCategory(owner, "  soups ", 42))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void everyCategoryMustBelongToARestaurant() {
        migrateTo("5");

        assertThatThrownBy(() -> insertCategory(null, "Orphans", 99))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void migrateTo(String version) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .createSchemas(false)
                .locations("classpath:db/migration")
                .target(version)
                .load()
                .migrate();
    }

    private void runSeedAgain() {
        new ResourceDatabasePopulator(new ClassPathResource(SEED_SCRIPT)).execute(dataSource);
    }

    private List<String> seedSnapshot() {
        return jdbc.queryForList("""
                SELECT c.name || '|' || c.display_order || '|' || i.name || '|' || i.price || '|'
                       || COALESCE(i.image_url, '') || '|' || i.updated_at
                FROM menu_items i JOIN menu_categories c ON c.id = i.category_id
                ORDER BY i.id
                """, String.class);
    }

    private long insertRestaurant(String name) {
        return jdbc.queryForObject("""
                INSERT INTO restaurants (name, owner_name, active, created_at, updated_at)
                VALUES (?, 'Owner', TRUE, NOW(), NOW()) RETURNING id
                """, Long.class, name);
    }

    // restaurantId is ignored before V5, when the column does not exist yet.
    private long insertCategory(Long restaurantId, String name, int displayOrder) {
        boolean hasRestaurantColumn = count("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'menu_categories'
                  AND column_name = 'restaurant_id'
                """) > 0;
        if (!hasRestaurantColumn) {
            return jdbc.queryForObject("""
                    INSERT INTO menu_categories (name, display_order, active, created_at, updated_at)
                    VALUES (?, ?, TRUE, NOW(), NOW()) RETURNING id
                    """, Long.class, name, displayOrder);
        }
        return jdbc.queryForObject("""
                INSERT INTO menu_categories (restaurant_id, name, display_order, active, created_at, updated_at)
                VALUES (?, ?, ?, TRUE, NOW(), NOW()) RETURNING id
                """, Long.class, restaurantId, name, displayOrder);
    }

    private long insertItem(String name, long categoryId) {
        return jdbc.queryForObject("""
                INSERT INTO menu_items (name, price, category_id, available, preparation_time_minutes,
                                        food_type, spice_level, created_at, updated_at)
                VALUES (?, 100.00, ?, TRUE, 10, 'VEG', 'MILD', NOW(), NOW()) RETURNING id
                """, Long.class, name, categoryId);
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }
}
