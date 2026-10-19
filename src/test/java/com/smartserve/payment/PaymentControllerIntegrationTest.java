package com.smartserve.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.entity.OrderItem;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.enums.OrderType;
import com.smartserve.order.enums.PaymentStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.order.repository.OrderItemRepository;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.entity.Restaurant;
import com.smartserve.restaurant.entity.RestaurantTable;
import com.smartserve.restaurant.enums.TableStatus;
import com.smartserve.restaurant.repository.BranchRepository;
import com.smartserve.restaurant.repository.RestaurantRepository;
import com.smartserve.restaurant.repository.RestaurantTableRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Runs with open-in-view disabled (test profile), like production, so the
 * verify response must be mapped inside a transaction.
 */
@SpringBootTest(properties = "razorpay.key-secret=" + PaymentControllerIntegrationTest.KEY_SECRET)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PaymentControllerIntegrationTest {

    static final String KEY_SECRET = "test-razorpay-key-secret";
    private static final String TRACKING_TOKEN = "track-verify-test";
    private static final String RAZORPAY_ORDER_ID = "order_TEST123";
    private static final String RAZORPAY_PAYMENT_ID = "pay_TEST456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private MenuCategoryRepository categoryRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private RestaurantTableRepository tableRepository;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        menuItemRepository.deleteAll();
        categoryRepository.deleteAll();
        tableRepository.deleteAll();
        branchRepository.deleteAll();
        restaurantRepository.deleteAll();
    }

    @Test
    void verifyReturnsPaidOrderWithItemsBranchAndTable() throws Exception {
        savePendingPaymentOrder();

        mockMvc.perform(post("/api/guest/orders/{token}/payment/verify", TRACKING_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson(sign(RAZORPAY_ORDER_ID + "|" + RAZORPAY_PAYMENT_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.branchName").value("Verify Branch"))
                .andExpect(jsonPath("$.data.tableNumber").value("T1"))
                .andExpect(jsonPath("$.data.items[0].itemName").value("Paneer Tikka"));

        CustomerOrder saved = orderRepository.findByTrackingToken(TRACKING_TOKEN).orElseThrow();
        assertEquals(PaymentStatus.PAID, saved.getPaymentStatus());
        assertEquals(RAZORPAY_PAYMENT_ID, saved.getRazorpayPaymentId());
    }

    @Test
    void verifyRejectsInvalidSignature() throws Exception {
        savePendingPaymentOrder();

        mockMvc.perform(post("/api/guest/orders/{token}/payment/verify", TRACKING_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson("not-a-valid-signature")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payment verification failed"));
    }

    private void savePendingPaymentOrder() {
        Restaurant restaurant = new Restaurant();
        restaurant.setName("Verify Restaurant");
        restaurant.setOwnerName("Verify Owner");
        restaurant.setActive(true);
        restaurant = restaurantRepository.save(restaurant);

        Branch branch = new Branch();
        branch.setRestaurant(restaurant);
        branch.setName("Verify Branch");
        branch.setAddress("Test Address");
        branch.setPhone("9876543210");
        branch.setActive(true);
        branch = branchRepository.save(branch);

        RestaurantTable table = new RestaurantTable();
        table.setBranch(branch);
        table.setTableNumber("T1");
        table.setCapacity(4);
        table.setStatus(TableStatus.OCCUPIED);
        table = tableRepository.save(table);

        MenuCategory category = new MenuCategory();
        category.setRestaurant(restaurant);
        category.setName("Starters");
        category.setDisplayOrder(1);
        category = categoryRepository.save(category);

        MenuItem menuItem = new MenuItem();
        menuItem.setName("Paneer Tikka");
        menuItem.setPrice(new BigDecimal("220.00"));
        menuItem.setCategory(category);
        menuItem.setAvailable(true);
        menuItem.setPreparationTimeMinutes(10);
        menuItem.setFoodType(FoodType.VEG);
        menuItem.setSpiceLevel(SpiceLevel.MILD);
        menuItem = menuItemRepository.save(menuItem);

        CustomerOrder order = new CustomerOrder();
        order.setCustomerName("Guest");
        order.setBranch(branch);
        order.setTable(table);
        order.setOrderType(OrderType.DINE_IN);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setRazorpayOrderId(RAZORPAY_ORDER_ID);
        order.setTrackingToken(TRACKING_TOKEN);
        order.setTotalAmount(menuItem.getPrice());

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setMenuItem(menuItem);
        orderItem.setItemName(menuItem.getName());
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(menuItem.getPrice());
        orderItem.setLineTotal(menuItem.getPrice());
        order.getItems().add(orderItem);

        orderRepository.saveAndFlush(order);
    }

    private static String verifyJson(String signature) {
        return """
                {"razorpayOrderId":"%s","razorpayPaymentId":"%s","razorpaySignature":"%s"}
                """.formatted(RAZORPAY_ORDER_ID, RAZORPAY_PAYMENT_ID, signature);
    }

    private static String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(KEY_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
