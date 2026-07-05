package com.smartserve.restaurant.dto;

import java.time.Instant;

public record RestaurantResponse(
        Long id,
        String name,
        String ownerName,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
