package com.smartserve.restaurant.dto;

import java.time.Instant;

public record BranchResponse(
        Long id,
        Long restaurantId,
        String restaurantName,
        String name,
        String address,
        String phone,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
