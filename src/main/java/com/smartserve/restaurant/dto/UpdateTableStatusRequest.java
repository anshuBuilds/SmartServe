package com.smartserve.restaurant.dto;

import com.smartserve.restaurant.enums.TableStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTableStatusRequest(
        @NotNull(message = "Table status is required")
        TableStatus status
) {
}
