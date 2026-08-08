package com.smartserve.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.smartserve.common.exception.BadRequestException;
import com.smartserve.notification.service.NotificationService;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.PaymentStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.payment.config.RazorpayProperties;
import com.smartserve.payment.service.RazorpayWebhookService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RazorpayWebhookServiceTest {

    @Mock CustomerOrderRepository orderRepository;
    @Mock NotificationService notificationService;

    private RazorpayWebhookService service;

    @BeforeEach
    void setUp() {
        service = serviceWith(new RazorpayProperties(
                "rzp-key", "payment-secret", "INR", "webhook-secret"
        ));
    }

    @Test
    void missingWebhookSecretIsAConfigurationError() {
        service = serviceWith(new RazorpayProperties("key", "secret", "INR", " "));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.processWebhook("{}", "signature")
        );

        assertEquals("Razorpay webhook secret is not configured", exception.getMessage());
    }

    @Test
    void invalidWebhookSignatureIsRejectedBeforePayloadIsProcessed() throws Exception {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.verifyWebhookSignature("{}", "bad", "webhook-secret"))
                    .thenThrow(new RazorpayException("invalid"));

            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> service.processWebhook("{}", "bad")
            );

            assertEquals("Invalid Razorpay webhook signature", exception.getMessage());
            verify(orderRepository, never()).findByRazorpayOrderId(any());
        }
    }

    @Test
    void unrelatedValidEventIsIgnored() throws Exception {
        String body = "{\"event\":\"refund.processed\"}";
        try (MockedStatic<Utils> utils = validSignature(body)) {
            service.processWebhook(body, "signature");
        }

        verify(orderRepository, never()).findByRazorpayOrderId(any());
    }

    @Test
    void malformedCapturedEventReturnsBadRequest() throws Exception {
        String body = "{\"event\":\"payment.captured\",\"payload\":{}}";
        try (MockedStatic<Utils> utils = validSignature(body)) {
            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> service.processWebhook(body, "signature")
            );
            assertEquals("Invalid Razorpay webhook payload", exception.getMessage());
        }
    }

    @Test
    void capturedPaymentMustMatchExistingSmartServeOrder() throws Exception {
        String body = capturedBody(12345, "INR");
        when(orderRepository.findByRazorpayOrderId("order_provider_1")).thenReturn(Optional.empty());

        try (MockedStatic<Utils> utils = validSignature(body)) {
            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> service.processWebhook(body, "signature")
            );
            assertEquals("No SmartServe order matches the Razorpay order", exception.getMessage());
        }
    }

    @Test
    void capturedPaymentRejectsWrongAmount() throws Exception {
        String body = capturedBody(9999, "INR");
        CustomerOrder order = order(PaymentStatus.PENDING);
        when(orderRepository.findByRazorpayOrderId("order_provider_1")).thenReturn(Optional.of(order));

        try (MockedStatic<Utils> utils = validSignature(body)) {
            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> service.processWebhook(body, "signature")
            );
            assertEquals("Razorpay payment amount does not match the order total", exception.getMessage());
        }

        verify(orderRepository, never()).save(any());
    }

    @Test
    void capturedPaymentRejectsWrongCurrency() throws Exception {
        String body = capturedBody(12345, "USD");
        CustomerOrder order = order(PaymentStatus.PENDING);
        when(orderRepository.findByRazorpayOrderId("order_provider_1")).thenReturn(Optional.of(order));

        try (MockedStatic<Utils> utils = validSignature(body)) {
            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> service.processWebhook(body, "signature")
            );
            assertEquals("Razorpay payment currency does not match", exception.getMessage());
        }

        verify(orderRepository, never()).save(any());
    }

    @Test
    void duplicateCapturedWebhookIsIdempotent() throws Exception {
        String body = capturedBody(12345, "INR");
        CustomerOrder order = order(PaymentStatus.PAID);
        when(orderRepository.findByRazorpayOrderId("order_provider_1")).thenReturn(Optional.of(order));

        try (MockedStatic<Utils> utils = validSignature(body)) {
            service.processWebhook(body, "signature");
        }

        verify(orderRepository, never()).save(any());
        verify(notificationService, never()).notifyOrderCreated(any());
    }

    @Test
    void validCapturedPaymentMarksOrderPaidAndNotifiesBranch() throws Exception {
        String body = capturedBody(12345, "inr");
        CustomerOrder order = order(PaymentStatus.PENDING);
        order.setPaymentFailureReason("previous attempt failed");
        when(orderRepository.findByRazorpayOrderId("order_provider_1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        try (MockedStatic<Utils> utils = validSignature(body)) {
            service.processWebhook(body, "signature");
        }

        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals("pay_1", order.getRazorpayPaymentId());
        assertEquals(null, order.getPaymentFailureReason());
        assertNotNull(order.getPaidAt());
        verify(orderRepository).save(order);
        verify(notificationService).notifyOrderCreated(order);
    }

    private RazorpayWebhookService serviceWith(RazorpayProperties properties) {
        return new RazorpayWebhookService(properties, orderRepository, notificationService);
    }

    private MockedStatic<Utils> validSignature(String body) throws Exception {
        MockedStatic<Utils> utils = mockStatic(Utils.class);
        utils.when(() -> Utils.verifyWebhookSignature(body, "signature", "webhook-secret"))
                .thenReturn(true);
        return utils;
    }

    private CustomerOrder order(PaymentStatus status) {
        CustomerOrder order = new CustomerOrder();
        ReflectionTestUtils.setField(order, "id", 10L);
        order.setRazorpayOrderId("order_provider_1");
        order.setTotalAmount(new BigDecimal("123.45"));
        order.setPaymentStatus(status);
        return order;
    }

    private String capturedBody(long amount, String currency) {
        return "{"
                + "\"event\":\"payment.captured\","
                + "\"payload\":{\"payment\":{\"entity\":{"
                + "\"id\":\"pay_1\","
                + "\"order_id\":\"order_provider_1\","
                + "\"amount\":" + amount + ","
                + "\"currency\":\"" + currency + "\""
                + "}}}}";
    }
}
