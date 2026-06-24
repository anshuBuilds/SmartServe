package com.smartserve.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderItemResponse(
        Long id,
        Long menuItemId,
        String itemName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        Instant createdAt,
        Instant updatedAt
) {
}
