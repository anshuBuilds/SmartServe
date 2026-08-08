package com.smartserve.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.smartserve.common.exception.BadRequestException;
import com.smartserve.common.exception.ConflictException;
import com.smartserve.notification.service.NotificationService;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.PaymentStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.payment.config.RazorpayProperties;
import com.smartserve.payment.dto.VerifyPaymentRequest;
import com.smartserve.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.Optional;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock CustomerOrderRepository orderRepository;
    @Mock NotificationService notificationService;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(
                new RazorpayProperties("rzp-key", "payment-secret", "INR", "webhook-secret"),
                orderRepository,
                notificationService
        );
    }

    @Test
    void createRazorpayOrderRejectsMissingOrNonPositiveAmount() {
        CustomerOrder order = order(new BigDecimal("0.00"), PaymentStatus.PENDING);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createRazorpayOrder(order)
        );

        assertEquals("Order amount must be greater than zero", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createRazorpayOrderRejectsAlreadyPaidOrder() {
        CustomerOrder order = order(new BigDecimal("10.00"), PaymentStatus.PAID);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.createRazorpayOrder(order)
        );

        assertEquals("Order has already been paid", exception.getMessage());
    }

    @Test
    void createRazorpayOrderConvertsAmountMasksPhoneAndPersistsProviderId() throws Exception {
        CustomerOrder order = order(new BigDecimal("123.45"), PaymentStatus.NOT_REQUIRED);
        OrderClient orderClient = mock(OrderClient.class);
        Order razorpayOrder = new Order(new JSONObject().put("id", "order_provider_1"));
        when(orderClient.create(any(JSONObject.class))).thenReturn(razorpayOrder);
        when(orderRepository.save(order)).thenReturn(order);

        try (MockedConstruction<RazorpayClient> ignored = mockConstruction(
                RazorpayClient.class,
                (mock, context) -> mock.orders = orderClient
        )) {
            var response = service.createRazorpayOrder(order);

            assertEquals("order_provider_1", order.getRazorpayOrderId());
            assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
            assertEquals(12345, response.amountInPaise());
            assertEquals(new BigDecimal("123.45"), response.amount());
            assertEquals("INR", response.currency());
            assertEquals("******3210", response.customerPhoneMasked());
            assertEquals("rzp-key", response.razorpayKeyId());
            verify(orderRepository).save(order);
        }
    }

    @Test
    void createRazorpayOrderMarksFailureWhenGatewayRejectsRequest() throws Exception {
        CustomerOrder order = order(new BigDecimal("123.45"), PaymentStatus.PENDING);
        OrderClient orderClient = mock(OrderClient.class);
        when(orderClient.create(any(JSONObject.class))).thenThrow(new RazorpayException("gateway down"));

        try (MockedConstruction<RazorpayClient> ignored = mockConstruction(
                RazorpayClient.class,
                (mock, context) -> mock.orders = orderClient
        )) {
            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> service.createRazorpayOrder(order)
            );

            assertEquals("Unable to create payment order", exception.getMessage());
            assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
            assertEquals("gateway down", order.getPaymentFailureReason());
            verify(orderRepository, never()).save(any());
        }
    }

    @Test
    void createRazorpayOrderRejectsAmountWithFractionalPaise() {
        CustomerOrder order = order(new BigDecimal("10.001"), PaymentStatus.PENDING);

        assertThrows(ArithmeticException.class, () -> service.createRazorpayOrder(order));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void verifyPaymentRejectsUnknownTrackingToken() {
        when(orderRepository.findByTrackingToken("missing")).thenReturn(Optional.empty());

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.verifyGuestPayment("missing", verifyRequest())
        );

        assertEquals("Order not found", exception.getMessage());
    }

    @Test
    void verifyPaymentRejectsAlreadyPaidOrder() {
        CustomerOrder order = order(new BigDecimal("123.45"), PaymentStatus.PAID);
        when(orderRepository.findByTrackingToken("track-123")).thenReturn(Optional.of(order));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.verifyGuestPayment("track-123", verifyRequest())
        );

        assertEquals("Order is already paid", exception.getMessage());
    }

    @Test
    void verifyPaymentRequiresInitializedMatchingProviderOrder() {
        CustomerOrder order = order(new BigDecimal("123.45"), PaymentStatus.PENDING);
        order.setRazorpayOrderId(null);
        when(orderRepository.findByTrackingToken("track-123")).thenReturn(Optional.of(order));

        BadRequestException missing = assertThrows(
                BadRequestException.class,
                () -> service.verifyGuestPayment("track-123", verifyRequest())
        );
        assertEquals("Payment was not initialized for this order", missing.getMessage());

        order.setRazorpayOrderId("different-order");
        BadRequestException mismatch = assertThrows(
                BadRequestException.class,
                () -> service.verifyGuestPayment("track-123", verifyRequest())
        );
        assertEquals("Payment order does not match", mismatch.getMessage());
    }

    @Test
    void validPaymentSignatureMarksOrderPaidAndNotifiesBranch() throws Exception {
        CustomerOrder order = order(new BigDecimal("123.45"), PaymentStatus.PENDING);
        when(orderRepository.findByTrackingToken("track-123")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), any(String.class)))
                    .thenReturn(true);

            CustomerOrder result = service.verifyGuestPayment("track-123", verifyRequest());

            assertSame(order, result);
            assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
            assertEquals("pay_1", order.getRazorpayPaymentId());
            assertEquals("signed", order.getRazorpaySignature());
            assertNull(order.getPaymentFailureReason());
            assertNotNull(order.getPaidAt());
            verify(notificationService).notifyOrderCreated(order);
        }
    }

    @Test
    void invalidPaymentSignatureMarksOrderFailedWithoutNotification() throws Exception {
        CustomerOrder order = order(new BigDecimal("123.45"), PaymentStatus.PENDING);
        when(orderRepository.findByTrackingToken("track-123")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), any(String.class)))
                    .thenThrow(new RazorpayException("bad signature"));

            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> service.verifyGuestPayment("track-123", verifyRequest())
            );

            assertEquals("Payment verification failed", exception.getMessage());
            assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
            assertEquals("Payment signature verification failed", order.getPaymentFailureReason());
            verify(orderRepository).save(order);
            verify(notificationService, never()).notifyOrderCreated(any());
        }
    }

    private CustomerOrder order(BigDecimal amount, PaymentStatus status) {
        CustomerOrder order = new CustomerOrder();
        ReflectionTestUtils.setField(order, "id", 10L);
        order.setTrackingToken("track-123");
        order.setCustomerName("Anshu");
        order.setCustomerPhone("9876543210");
        order.setTotalAmount(amount);
        order.setPaymentStatus(status);
        order.setRazorpayOrderId("order_provider_1");
        return order;
    }

    private VerifyPaymentRequest verifyRequest() {
        VerifyPaymentRequest request = new VerifyPaymentRequest();
        request.setRazorpayOrderId("order_provider_1");
        request.setRazorpayPaymentId("pay_1");
        request.setRazorpaySignature("signed");
        return request;
    }
}
