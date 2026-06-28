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
import java.math.BigDecimal;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MenuControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateCategory() throws Exception {
        mockMvc.perform(post("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Starters", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Starters"))
                .andExpect(jsonPath("$.data.displayOrder").value(1));
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
    void duplicateCategoryNameIsRejected() throws Exception {
        categoryRepository.save(category("Starters", 1));

        mockMvc.perform(post("/api/menu/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Starters", 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Category already exists"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidMenuItemReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/menu/items")
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
    void categoryContainingItemsCannotBeDeleted() throws Exception {
        MenuCategory category = categoryRepository.save(category("Mains", 1));
        itemRepository.save(item("Paneer", category, true));

        mockMvc.perform(delete("/api/menu/categories/{id}", category.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Cannot delete a category that has menu items"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotDeleteCategory() throws Exception {
        MenuCategory category = categoryRepository.save(category("Desserts", 1));

        mockMvc.perform(delete("/api/menu/categories/{id}", category.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void availableOnlyFilterExcludesUnavailableItems() throws Exception {
        MenuCategory category = categoryRepository.save(category("Drinks", 1));
        itemRepository.save(item("Tea", category, true));
        itemRepository.save(item("Seasonal Shake", category, false));

        mockMvc.perform(get("/api/menu/items")
                        .param("categoryId", category.getId().toString())
                        .param("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Tea"));
    }

    private MenuCategory category(String name, int displayOrder) {
        MenuCategory category = new MenuCategory();
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
}
