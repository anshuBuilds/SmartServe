package com.smartserve.order.dto;

import com.smartserve.order.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Integer tableNumber,
        String customerName,
        OrderStatus orderStatus,
        BigDecimal totalAmount,
        String specialInstructions,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
