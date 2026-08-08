package com.smartserve.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartserve.common.exception.GlobalExceptionHandler;
import com.smartserve.guest.controller.GuestController;
import com.smartserve.guest.dto.GuestOrderPaymentResponse;
import com.smartserve.guest.dto.GuestSessionResponse;
import com.smartserve.guest.service.GuestService;
import com.smartserve.notification.controller.NotificationController;
import com.smartserve.notification.dto.NotificationResponse;
import com.smartserve.notification.enums.NotificationChannel;
import com.smartserve.notification.enums.NotificationStatus;
import com.smartserve.notification.enums.NotificationType;
import com.smartserve.notification.service.NotificationService;
import com.smartserve.order.dto.OrderResponse;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.enums.OrderType;
import com.smartserve.order.enums.PaymentMethod;
import com.smartserve.order.enums.PaymentStatus;
import com.smartserve.order.service.OrderService;
import com.smartserve.payment.controller.PaymentController;
import com.smartserve.payment.controller.RazorpayWebhookController;
import com.smartserve.payment.service.PaymentService;
import com.smartserve.payment.service.RazorpayWebhookService;
import com.smartserve.restaurant.enums.TableStatus;
import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import com.smartserve.user.repository.UserRepository;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RemainingControllersWebTest {

    @Mock GuestService guestService;
    @Mock PaymentService paymentService;
    @Mock OrderService orderService;
    @Mock RazorpayWebhookService webhookService;
    @Mock NotificationService notificationService;
    @Mock UserRepository userRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new GuestController(guestService),
                        new PaymentController(paymentService, orderService),
                        new RazorpayWebhookController(webhookService),
                        new NotificationController(notificationService, userRepository)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void guestSessionEndpointReturnsPublicTableContext() throws Exception {
        when(guestService.session("qr-token")).thenReturn(new GuestSessionResponse(
                2L, "Main Branch", "SmartServe", 4L, "T4", TableStatus.AVAILABLE, true
        ));

        mockMvc.perform(get("/api/guest/session/qr-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tableNumber").value("T4"))
                .andExpect(jsonPath("$.data.orderingEnabled").value(true));
    }

    @Test
    void guestOrderEndpointValidatesRequestBody() throws Exception {
        mockMvc.perform(post("/api/guest/orders/qr-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.customerName").exists())
                .andExpect(jsonPath("$.validationErrors.items").exists());
    }

    @Test
    void validGuestOrderReturns201() throws Exception {
        GuestOrderPaymentResponse response = new GuestOrderPaymentResponse(
                "track-123", orderResponse(), null
        );
        when(guestService.createOrder(org.mockito.ArgumentMatchers.eq("qr-token"), any()))
                .thenReturn(response);
        String requestBody = """
                {
                  "customerName": "Anshu",
                  "customerPhone": "9876543210",
                  "smsConsent": true,
                  "items": [{"menuItemId": 8, "quantity": 2}]
                }
                """;

        mockMvc.perform(post("/api/guest/orders/qr-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Order created"))
                .andExpect(jsonPath("$.data.trackingToken").value("track-123"));
    }

    @Test
    void paymentVerificationEndpointMapsPaidOrderResponse() throws Exception {
        CustomerOrder paidOrder = new CustomerOrder();
        when(paymentService.verifyGuestPayment(org.mockito.ArgumentMatchers.eq("track-123"), any()))
                .thenReturn(paidOrder);
        when(orderService.mapOrder(paidOrder)).thenReturn(orderResponse());
        String requestBody = """
                {
                  "razorpayOrderId": "order_1",
                  "razorpayPaymentId": "pay_1",
                  "razorpaySignature": "signed"
                }
                """;

        mockMvc.perform(post("/api/guest/orders/track-123/payment/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment verified"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));
    }

    @Test
    void paymentVerificationRejectsBlankProviderFields() throws Exception {
        mockMvc.perform(post("/api/guest/orders/track-123/payment/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.razorpayOrderId").exists())
                .andExpect(jsonPath("$.validationErrors.razorpayPaymentId").exists())
                .andExpect(jsonPath("$.validationErrors.razorpaySignature").exists());
    }

    @Test
    void webhookEndpointPassesUntouchedBodyAndSignatureToVerifier() throws Exception {
        String body = "{\"event\":\"payment.captured\"}";

        mockMvc.perform(post("/api/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(webhookService).processWebhook(body, "signature");
    }

    @Test
    void notificationsEndpointUsesAuthenticatedPrincipalUserId() throws Exception {
        UserEntity user = user(11L, "chef");
        NotificationResponse notification = new NotificationResponse(
                20L, 2L, "Main Branch", 10L, 11L, NotificationChannel.IN_APP,
                NotificationType.ORDER_READY, "Order ready", "Table T4 order is ready to serve.",
                NotificationStatus.PENDING, null, Instant.now(), null, null
        );
        when(userRepository.findByUsername("chef")).thenReturn(Optional.of(user));
        when(notificationService.getMyNotifications(11L)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications").principal(principal("chef")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("ORDER_READY"))
                .andExpect(jsonPath("$.data[0].recipientUserId").value(11));
    }

    @Test
    void markAllNotificationsReadDelegatesForAuthenticatedUser() throws Exception {
        when(userRepository.findByUsername("chef")).thenReturn(Optional.of(user(11L, "chef")));

        mockMvc.perform(patch("/api/notifications/read-all").principal(principal("chef")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notifications marked as read"));

        verify(notificationService).markAllAsRead(11L);
    }

    @Test
    void notificationEndpointReturns404WhenPrincipalNoLongerExists() throws Exception {
        when(userRepository.findByUsername("deleted-user")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/notifications").principal(principal("deleted-user")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Current user not found"));
    }

    private Principal principal(String name) {
        return () -> name;
    }

    private UserEntity user(Long id, String username) {
        UserEntity user = new UserEntity();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        user.setRole(Role.KITCHEN);
        user.setActive(true);
        return user;
    }

    private OrderResponse orderResponse() {
        return new OrderResponse(
                10L, 2L, "Main Branch", 4L, "T4", "Anshu", OrderType.DINE_IN,
                "******3210", true, OrderStatus.PENDING, PaymentStatus.PAID, PaymentMethod.RAZORPAY,
                Instant.now(), new BigDecimal("250.00"), null, List.of(), Instant.now(), Instant.now()
        );
    }
}
