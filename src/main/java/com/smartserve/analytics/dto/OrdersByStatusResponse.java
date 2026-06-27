package com.smartserve.analytics.dto;

import com.smartserve.order.enums.OrderStatus;
import java.math.BigDecimal;

public record OrdersByStatusResponse(
        OrderStatus status,
        Long orderCount,
        BigDecimal totalAmount
) {
}