package com.smartserve.menu;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartserve.TestcontainersConfiguration;
import com.smartserve.menu.entity.MenuCategory;
import com.smartserve.menu.entity.MenuItem;
import com.smartserve.menu.enums.FoodType;
import com.smartserve.menu.enums.SpiceLevel;
import com.smartserve.menu.repository.MenuCategoryRepository;
import com.smartserve.menu.repository.MenuItemRepository;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.order.repository.OrderItemRepository;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.entity.Restaurant;
import com.smartserve.restaurant.entity.RestaurantTable;
import com.smartserve.restaurant.repository.BranchRepository;
import com.smartserve.restaurant.repository.RestaurantRepository;
import com.smartserve.restaurant.repository.RestaurantTableRepository;
import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import com.smartserve.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Menus belong to a restaurant: two restaurants are set up and every test checks that one
 * restaurant's staff, guests and ids never reach the other's menu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MenuControllerIntegrationTest {

    private static final List<String> STAFF = List.of("waiter-a", "manager-a");

    @Autowired private MockMvc mockMvc;
    @Autowired private MenuCategoryRepository categoryRepository;
    @Autowired private MenuItemRepository itemRepository;
    @Autowired private CustomerOrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private RestaurantTableRepository tableRepository;
    @Autowired private UserRepository userRepository;

    private Restaurant restaurantA;
    private Restaurant restaurantB;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        restaurantA = restaurant("Spice Route");
        restaurantB = restaurant("Coastal Kitchen");
        // Restaurant A has two branches sharing one menu.
        Branch a1 = branch(restaurantA, "Spice Route Andheri");
        Branch a2 = branch(restaurantA, "Spice Route Bandra");
        branch(restaurantB, "Coastal Kitchen Juhu");
        staff("waiter-a", Role.WAITER, a1);
        staff("manager-a", Role.MANAGER, a2);
    }

    @AfterEach
    void cleanDatabase() {
        // Staff reference branches, and menus and orders reference restaurants, so delete
        // in dependency order. Only this class's staff are removed.
        STAFF.forEach(name -> userRepository.findByUsername(name).ifPresent(userRepository::delete));
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        itemRepository.deleteAll();
        categoryRepository.deleteAll();
        tableRepository.deleteAll();
        branchRepository.deleteAll();
        restaurantRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCreatesCategoryForChosenRestaurant() throws Exception {
        mockMvc.perform(post("/api/menu/categories")
                        .param("restaurantId", restaurantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Starters", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Starters"))
                .andExpect(jsonPath("$.data.restaurantId").value(restaurantA.getId()))
                .andExpect(jsonPath("$.data.displayOrder").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminMustChooseARestaurant() throws Exception {
        mockMvc.perform(get("/api/menu/items"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("restaurantId is required for administrators"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unknownRestaurantIsNotFound() throws Exception {
        mockMvc.perform(get("/api/menu/items").param("restaurantId", "999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Restaurant not found"));
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void waiterCannotCreateCategory() throws Exception {
        mockMvc.perform(post("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Starters", 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateCategoryNameInSameRestaurantIsRejectedIgnoringCase() throws Exception {
        categoryRepository.save(category(restaurantA, "Starters", 1));

        mockMvc.perform(post("/api/menu/categories")
                        .param("restaurantId", restaurantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("starters", 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category already exists"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sameCategoryNameAndOrderAreAllowedInAnotherRestaurant() throws Exception {
        categoryRepository.save(category(restaurantA, "Starters", 1));

        mockMvc.perform(post("/api/menu/categories")
                        .param("restaurantId", restaurantB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Starters", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.restaurantId").value(restaurantB.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void negativeCategoryDisplayOrderIsRejected() throws Exception {
        mockMvc.perform(post("/api/menu/categories")
                        .param("restaurantId", restaurantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Invalid order", -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.displayOrder")
                        .value("Display order must be zero or greater"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidMenuItemReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/menu/items")
                        .param("restaurantId", restaurantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "price": 0,
                                  "preparationTimeMinutes": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.categoryId").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateItemNameInSameCategoryIsRejectedIgnoringCaseAndSpaces() throws Exception {
        MenuCategory category = categoryRepository.save(category(restaurantA, "Special Cuisine", 1));
        itemRepository.save(item("Chut Chole", category, true));

        mockMvc.perform(post("/api/menu/items")
                        .param("restaurantId", restaurantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson("  chut chole  ", category.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("A menu item with this name already exists in the category"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void categoryContainingItemsCannotBeDeleted() throws Exception {
        MenuCategory category = categoryRepository.save(category(restaurantA, "Mains", 1));
        itemRepository.save(item("Paneer", category, true));

        mockMvc.perform(delete("/api/menu/categories/{id}", category.getId())
                        .param("restaurantId", restaurantA.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete a category that has menu items"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotDeleteCategory() throws Exception {
        MenuCategory category = categoryRepository.save(category(restaurantA, "Desserts", 1));

        mockMvc.perform(delete("/api/menu/categories/{id}", category.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "waiter-a", roles = "WAITER")
    void staffSeeOnlyTheirRestaurantsAvailableItems() throws Exception {
        MenuCategory drinks = categoryRepository.save(category(restaurantA, "Drinks", 1));
        itemRepository.save(item("Tea", drinks, true));
        itemRepository.save(item("Seasonal Shake", drinks, false));
        MenuCategory otherDrinks = categoryRepository.save(category(restaurantB, "Drinks", 1));
        itemRepository.save(item("Coconut Water", otherDrinks, true));

        mockMvc.perform(get("/api/menu/items").param("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Tea"));

        mockMvc.perform(get("/api/menu/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].restaurantId").value(restaurantA.getId()));
    }

    @Test
    @WithMockUser(username = "manager-a", roles = "MANAGER")
    void managerAddsDishToTheMenuSharedByTheirRestaurantsBranches() throws Exception {
        MenuCategory mains = categoryRepository.save(category(restaurantA, "Mains", 1));

        mockMvc.perform(post("/api/menu/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson("Butter Chicken", mains.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.categoryId").value(mains.getId()));
    }

    @Test
    @WithMockUser(username = "manager-a", roles = "MANAGER")
    void managerCannotReachAnotherRestaurantsMenu() throws Exception {
        MenuCategory otherMains = categoryRepository.save(category(restaurantB, "Mains", 1));
        MenuItem otherDish = itemRepository.save(item("Fish Curry", otherMains, true));

        // Asking for the other restaurant explicitly is forbidden.
        mockMvc.perform(get("/api/menu/items").param("restaurantId", restaurantB.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only access your own restaurant's menu"));

        // Using the other restaurant's ids reveals nothing: they simply don't exist here.
        mockMvc.perform(post("/api/menu/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson("Sneaky Dish", otherMains.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Menu category not found"));
        mockMvc.perform(get("/api/menu/items/{id}", otherDish.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Menu item not found"));
    }

    @Test
    void guestMenuShowsOnlyTheTablesRestaurant() throws Exception {
        MenuCategory starters = categoryRepository.save(category(restaurantA, "Starters", 1));
        itemRepository.save(item("Samosa", starters, true));
        MenuCategory otherStarters = categoryRepository.save(category(restaurantB, "Starters", 1));
        itemRepository.save(item("Prawn Koliwada", otherStarters, true));
        Branch bandra = branchRepository.findAll().stream()
                .filter(b -> b.getName().equals("Spice Route Bandra"))
                .findFirst()
                .orElseThrow();
        RestaurantTable table = new RestaurantTable();
        table.setBranch(bandra);
        table.setTableNumber("T1");
        table.setCapacity(4);
        table.setQrToken("qr-spice-route-bandra-t1");
        tableRepository.save(table);

        mockMvc.perform(get("/api/guest/menu/{token}", "qr-spice-route-bandra-t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories.length()").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("Samosa"));
    }

    private Restaurant restaurant(String name) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(name);
        restaurant.setOwnerName("Owner");
        restaurant.setActive(true);
        return restaurantRepository.save(restaurant);
    }

    private Branch branch(Restaurant restaurant, String name) {
        Branch branch = new Branch();
        branch.setRestaurant(restaurant);
        branch.setName(name);
        branch.setAddress("Mumbai");
        branch.setPhone("9876543210");
        branch.setActive(true);
        return branchRepository.save(branch);
    }

    private void staff(String username, Role role, Branch branch) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword("not-used-by-mock-auth");
        user.setFullName(username);
        user.setRole(role);
        user.setActive(true);
        user.setBranch(branch);
        userRepository.save(user);
    }

    private MenuCategory category(Restaurant restaurant, String name, int displayOrder) {
        MenuCategory category = new MenuCategory();
        category.setRestaurant(restaurant);
        category.setName(name);
        category.setDisplayOrder(displayOrder);
        category.setActive(true);
        return category;
    }

    private MenuItem item(String name, MenuCategory category, boolean available) {
        MenuItem item = new MenuItem();
        item.setName(name);
        item.setPrice(new BigDecimal("100.00"));
        item.setCategory(category);
        item.setAvailable(available);
        item.setPreparationTimeMinutes(10);
        item.setFoodType(FoodType.VEG);
        item.setSpiceLevel(SpiceLevel.MILD);
        return item;
    }

    private String categoryJson(String name, int displayOrder) {
        return """
                {
                  "categoryName": "%s",
                  "description": "Test category",
                  "displayOrder": %d
                }
                """.formatted(name, displayOrder);
    }

    private String itemJson(String name, Long categoryId) {
        return """
                {
                  "name": "%s",
                  "description": "Test item",
                  "price": 50.00,
                  "categoryId": %d,
                  "preparationTimeMinutes": 10,
                  "foodType": "VEG",
                  "spiceLevel": "MEDIUM"
                }
                """.formatted(name, categoryId);
    }
}
