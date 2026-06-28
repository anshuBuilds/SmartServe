package com.smartserve.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smartserve.TestcontainersConfiguration;
import com.smartserve.analytics.dto.OrdersByStatusResponse;
import com.smartserve.analytics.dto.SalesSummaryResponse;
import com.smartserve.analytics.dto.TablePerformanceResponse;
import com.smartserve.analytics.dto.TopMenuItemResponse;
import com.smartserve.analytics.service.AnalyticsService;
import com.smartserve.menu.entity.MenuCategory;
import com.smartserve.menu.entity.MenuItem;
import com.smartserve.menu.enums.FoodType;
import com.smartserve.menu.enums.SpiceLevel;
import com.smartserve.menu.repository.MenuCategoryRepository;
import com.smartserve.menu.repository.MenuItemRepository;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.entity.OrderItem;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.order.repository.OrderItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AnalyticsServiceIntegrationTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private MenuCategoryRepository categoryRepository;

    private MenuItem burger;
    private MenuItem fries;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        menuItemRepository.deleteAll();
        categoryRepository.deleteAll();

        MenuCategory category = new MenuCategory();
        category.setName("Analytics Menu");
        category.setDisplayOrder(1);
        category = categoryRepository.save(category);
        burger = menuItemRepository.save(menuItem("Burger", "100.00", category));
        fries = menuItemRepository.save(menuItem("Fries", "50.00", category));
    }

    @Test
    void salesSummaryCountsRevenueFromServedOrdersOnly() {
        saveOrder(1, OrderStatus.SERVED, burger, 2);
        saveOrder(2, OrderStatus.SERVED, fries, 1);
        saveOrder(3, OrderStatus.PENDING, burger, 5);
        saveOrder(4, OrderStatus.CANCELLED, burger, 1);

        SalesSummaryResponse result = analyticsService.getSalesSummary(null, null);

        assertEquals(new BigDecimal("250.00"), result.totalRevenue());
        assertEquals(2L, result.servedOrderCount());
        assertEquals(4L, result.totalOrderCount());
        assertEquals(1L, result.cancelledOrderCount());
        assertEquals(new BigDecimal("125.00"), result.averageOrderValue());
    }

    @Test
    void topItemsRanksServedQuantitiesAndHonorsLimit() {
        saveOrder(1, OrderStatus.SERVED, burger, 2);
        saveOrder(2, OrderStatus.SERVED, fries, 4);
        saveOrder(3, OrderStatus.PENDING, burger, 20);

        List<TopMenuItemResponse> result = analyticsService.getTopSellingItems(null, null, 1);

        assertEquals(1, result.size());
        assertEquals("Fries", result.get(0).itemName());
        assertEquals(4L, result.get(0).quantitySold());
        assertEquals(new BigDecimal("200.00"), result.get(0).revenue());
    }

    @Test
    void ordersByStatusIncludesZeroRowsForMissingStatuses() {
        saveOrder(1, OrderStatus.SERVED, burger, 1);
        saveOrder(2, OrderStatus.PENDING, fries, 1);

        List<OrdersByStatusResponse> result = analyticsService.getOrdersByStatus(null, null);

        assertEquals(OrderStatus.values().length, result.size());
        assertEquals(1L, countFor(result, OrderStatus.SERVED));
        assertEquals(1L, countFor(result, OrderStatus.PENDING));
        assertEquals(0L, countFor(result, OrderStatus.CANCELLED));
    }

    @Test
    void tablePerformanceSortsByRevenueAndCalculatesAverage() {
        saveOrder(5, OrderStatus.SERVED, burger, 2);
        saveOrder(5, OrderStatus.SERVED, fries, 1);
        saveOrder(8, OrderStatus.SERVED, burger, 1);
        saveOrder(9, OrderStatus.PENDING, burger, 20);

        List<TablePerformanceResponse> result = analyticsService.getTablePerformance(null, null);

        assertEquals(2, result.size());
        assertEquals(5, result.get(0).tableNumber());
        assertEquals(2L, result.get(0).servedOrderCount());
        assertEquals(new BigDecimal("250.00"), result.get(0).revenue());
        assertEquals(new BigDecimal("125.00"), result.get(0).averageOrderValue());
    }

    @Test
    void invalidDateRangeAndLimitAreRejected() {
        Instant now = Instant.now();
        assertThrows(IllegalArgumentException.class,
                () -> analyticsService.getSalesSummary(now, now));
        assertThrows(IllegalArgumentException.class,
                () -> analyticsService.getTopSellingItems(null, null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> analyticsService.getTopSellingItems(null, null, 51));
    }

    private long countFor(List<OrdersByStatusResponse> results, OrderStatus status) {
        return results.stream()
                .filter(result -> result.status() == status)
                .findFirst()
                .orElseThrow()
                .orderCount();
    }

    private MenuItem menuItem(String name, String price, MenuCategory category) {
        MenuItem item = new MenuItem();
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        item.setCategory(category);
        item.setAvailable(true);
        item.setPreparationTimeMinutes(10);
        item.setFoodType(FoodType.VEG);
        item.setSpiceLevel(SpiceLevel.MILD);
        return item;
    }

    private void saveOrder(int tableNumber, OrderStatus status, MenuItem menuItem, int quantity) {
        CustomerOrder order = new CustomerOrder();
        order.setCustomerName("Table " + tableNumber);
        order.setTableNumber(tableNumber);
        order.setOrderStatus(status);

        BigDecimal lineTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(quantity));
        order.setTotalAmount(lineTotal);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setMenuItem(menuItem);
        orderItem.setItemName(menuItem.getName());
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(menuItem.getPrice());
        orderItem.setLineTotal(lineTotal);
        order.getItems().add(orderItem);

        orderRepository.saveAndFlush(order);
    }
}
