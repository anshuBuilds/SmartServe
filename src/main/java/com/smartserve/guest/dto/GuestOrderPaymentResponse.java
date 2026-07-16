package com.smartserve.guest.dto;

import com.smartserve.order.dto.OrderResponse;
import com.smartserve.payment.dto.PaymentOrderResponse;

public record GuestOrderPaymentResponse(
        String trackingToken,
        OrderResponse order,
        PaymentOrderResponse payment
) {
}