package com.smartserve.restaurant.dto;

import com.smartserve.restaurant.enums.TableStatus;
import java.time.Instant;

public record TableResponse(
        Long id,
        Long branchId,
        String tableNumber,
        Integer capacity,
        TableStatus status,
        String qrToken,
        Instant createdAt,
        Instant updatedAt
) {
}
