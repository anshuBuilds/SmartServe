package com.smartserve.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTableRequest(
        @NotBlank(message = "Table number is required")
        @Size(max = 20, message = "Table number must not exceed 20 characters")
        String tableNumber,

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        @Max(value = 50, message = "Capacity must not exceed 50")
        Integer capacity
) {
}
