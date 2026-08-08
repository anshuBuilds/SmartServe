package com.smartserve.payment.service;


import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.smartserve.common.exception.BadRequestException;
import com.smartserve.notification.service.NotificationService;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.PaymentStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.payment.config.RazorpayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private final RazorpayProperties razorpayProperties;
    private final CustomerOrderRepository customerOrderRepository;
    private final NotificationService notificationService;

    @Transactional
    public void processWebhook(
            String rawBody,
            String razorpaySignature
    ) {
        verifySignature(rawBody, razorpaySignature);

        try {
            JSONObject webhook = new JSONObject(rawBody);
            String eventType = webhook.optString("event");

            if (!"payment.captured".equals(eventType)) {
                log.debug(
                      "Ignoring Razorpay event: {}",
                        eventType
                );
                return;
            }

            JSONObject payment = webhook
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String razorpayPaymentId =
                    payment.getString("id");

            String razorpayOrderId =
                    payment.getString("order_id");

            long amountInPaise =
                    payment.getLong("amount");

            String currency =
                    payment.getString("currency");

            CustomerOrder order = customerOrderRepository
                    .findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() -> new BadRequestException(
                            "No SmartServe order matches the Razorpay order"
                    ));

            long expectedAmountInPaise = toPaise(order.getTotalAmount());

            if(amountInPaise != expectedAmountInPaise) {
                throw new BadRequestException(
                        "Razorpay payment amount does not match the order total"
                );
            }

            String expectedCurrency =
                    razorpayProperties.currency();

            if (expectedCurrency == null
                    || !expectedCurrency.equalsIgnoreCase(currency)) {
                throw new BadRequestException(
                        "Razorpay payment currency does not match"
                );
            }

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                log.info(
                        "SmartServe order {} is already paid; ignoring duplicate webhook",
                        order.getId()
                );
                return;
            }

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setRazorpayPaymentId(razorpayPaymentId);
            order.setPaymentFailureReason(null);
            order.setPaidAt(Instant.now());

            CustomerOrder savedOrder =
                    customerOrderRepository.save(order);

            notificationService.notifyOrderCreated(savedOrder);

            log.info(
                    "Razorpay payment {} marked SmartServe order {} as paid",
                    razorpayPaymentId,
                    savedOrder.getId()
            );
        } catch (JSONException e) {
            throw new BadRequestException(
                    "Invalid Razorpay webhook payload"
            );
        }
    }

    private void verifySignature(String rawBody, String razorPaySignature) {
        String webhookSecret = razorpayProperties.webhookSecret();

        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Razorpay webhook secret is not configured");
        }

        try {
            Utils.verifyWebhookSignature(
                    rawBody,
                    razorPaySignature,
                    webhookSecret
            );
        } catch (RazorpayException exception) {
            throw new BadRequestException(
                    "Invalid Razorpay webhook signature"
            );
        }
    }

    private long toPaise(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }
}
