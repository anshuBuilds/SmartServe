package com.smartserve.kitchen.dto;

import com.smartserve.order.enums.OrderStatus;
import java.time.Instant;
import java.util.List;

public record KitchenTicketResponse(
        Long orderId,
        Long branchId,
        String branchName,
        Long tableId,
        String tableNumber,
        String customerName,
        OrderStatus status,
        String specialInstructions,
        Instant createdAt,
        Instant preparationStartedAt,
        Instant readyAt,
        Integer estimatedPreparationMinutes,
        List<KitchenTicketItemResponse> items
) {}
