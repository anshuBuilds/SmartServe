package com.smartserve.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRestaurantRequest(
        @NotBlank(message = "Restaurant name is required")
        @Size(max = 120, message = "Restaurant name must not exceed 120 characters")
        String name,

        @NotBlank(message = "Owner name is required")
        @Size(max = 120, message = "Owner name must not exceed 120 characters")
        String ownerName
) {
}
