package com.smartserve.restaurant.dto;

import com.smartserve.restaurant.enums.TableStatus;
import java.time.Instant;

public record TableResponse(
        Long id,
        Long branchId,
        String tableNumber,
        Integer capacity,
        TableStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
