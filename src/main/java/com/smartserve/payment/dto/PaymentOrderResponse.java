package com.smartserve.payment.dto;

import java.math.BigDecimal;

public record PaymentOrderResponse(
        Long smartServeOrderId,
        String trackingToken,
        String razorpayOrderId,
        String razorpayKeyId,
        BigDecimal amount,
        Integer amountInPaise,
        String currency,
        String customerName,
        String customerPhoneMasked
) {
}
