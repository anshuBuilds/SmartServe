package com.smartserve.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartserve.TestcontainersConfiguration;
import com.smartserve.menu.entity.MenuCategory;
import com.smartserve.menu.entity.MenuItem;
import com.smartserve.menu.enums.FoodType;
import com.smartserve.menu.enums.SpiceLevel;
import com.smartserve.menu.repository.MenuCategoryRepository;
import com.smartserve.menu.repository.MenuItemRepository;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.order.repository.OrderItemRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

/**
 * The V4 seed migration must be safe to apply to a menu that already has data: it reuses
 * categories by name, never duplicates a dish name and changes nothing when run again.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class IndianMenuSeedMigrationIntegrationTest {

    private static final String SEED_SCRIPT = "db/migration/V4__seed_indian_menu.sql";
    private static final int SEED_CATEGORIES = 10;
    private static final int SEED_DISHES = 90;
    private static final String IMAGE_PREFIX = "https://smartserve.anshuyadav.me/menu/";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MenuCategoryRepository categoryRepository;

    @Autowired
    private MenuItemRepository itemRepository;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @BeforeEach
    void cleanDatabase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        itemRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void seedsEmptyMenu() {
        runSeed();

        assertThat(count("SELECT COUNT(*) FROM menu_categories")).isEqualTo(SEED_CATEGORIES);
        assertThat(count("SELECT COUNT(*) FROM menu_items")).isEqualTo(SEED_DISHES);
        assertThat(count("SELECT COUNT(*) FROM menu_items WHERE available = FALSE")).isZero();
    }

    @Test
    void secondRunChangesNothing() {
        runSeed();
        List<String> before = snapshot();

        runSeed();

        assertThat(snapshot()).isEqualTo(before);
        assertThat(count("SELECT COUNT(*) FROM menu_categories")).isEqualTo(SEED_CATEGORIES);
        assertThat(count("SELECT COUNT(*) FROM menu_items")).isEqualTo(SEED_DISHES);
    }

    @Test
    void keepsExistingCategoriesAndDoesNotDuplicateExistingDishNames() {
        MenuCategory mains = categoryRepository.save(category("main course", 2));
        MenuCategory specials = categoryRepository.save(category("Chef Specials", 0));
        // Same dish as the seed's "Butter Chicken", different case/whitespace and category.
        MenuItem existing = itemRepository.save(item("  butter CHICKEN ", specials));
        itemRepository.save(item("House Thali", mains));

        runSeed();
        runSeed();

        assertThat(count("SELECT COUNT(*) FROM menu_categories")).isEqualTo(SEED_CATEGORIES + 1);
        assertThat(count("SELECT COUNT(*) FROM menu_categories WHERE LOWER(name) = 'main course'"))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM menu_items")).isEqualTo(2 + SEED_DISHES - 1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT id FROM menu_items WHERE LOWER(BTRIM(name)) = 'butter chicken'", Long.class))
                .containsExactly(existing.getId());

        // Existing category is reused and moved into the standard menu order.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT display_order FROM menu_categories WHERE id = ?", Integer.class, mains.getId()))
                .isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM menu_items WHERE category_id = ?", mains.getId()))
                .isGreaterThan(1);
        // Categories outside the seed keep their order.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT display_order FROM menu_categories WHERE id = ?", Integer.class, specials.getId()))
                .isZero();
    }

    @Test
    void everySeededPhotoIsShippedWithTheFrontend() {
        runSeed();

        List<String> urls = jdbcTemplate.queryForList(
                "SELECT image_url FROM menu_items WHERE image_url IS NOT NULL", String.class);
        assertThat(urls).isNotEmpty();
        Path menuPhotos = Path.of("frontend", "public", "menu");
        for (String url : urls) {
            assertThat(url).startsWith(IMAGE_PREFIX);
            assertThat(menuPhotos.resolve(url.substring(IMAGE_PREFIX.length()))).isRegularFile();
        }
        assertThat(Files.isDirectory(menuPhotos)).isTrue();
    }

    private void runSeed() {
        new ResourceDatabasePopulator(new ClassPathResource(SEED_SCRIPT)).execute(dataSource);
    }

    private List<String> snapshot() {
        return jdbcTemplate.queryForList("""
                SELECT c.name || '|' || c.display_order || '|' || i.name || '|' || i.price || '|'
                       || COALESCE(i.image_url, '') || '|' || i.updated_at
                FROM menu_items i JOIN menu_categories c ON c.id = i.category_id
                ORDER BY i.id
                """, String.class);
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private MenuCategory category(String name, int displayOrder) {
        MenuCategory category = new MenuCategory();
        category.setName(name);
        category.setDisplayOrder(displayOrder);
        category.setActive(true);
        return category;
    }

    private MenuItem item(String name, MenuCategory category) {
        MenuItem item = new MenuItem();
        item.setName(name);
        item.setPrice(new BigDecimal("100.00"));
        item.setCategory(category);
        item.setAvailable(true);
        item.setPreparationTimeMinutes(10);
        item.setFoodType(FoodType.VEG);
        item.setSpiceLevel(SpiceLevel.MILD);
        return item;
    }
}
