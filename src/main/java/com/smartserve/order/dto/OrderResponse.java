package com.smartserve.order.dto;

import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.enums.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long branchId,
        String branchName,
        Long tableId,
        String tableNumber,
        String customerName,
        OrderType orderType,
        String customerPhoneMasked,
        Boolean smsConsent,
        OrderStatus orderStatus,
        BigDecimal totalAmount,
        String specialInstructions,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
