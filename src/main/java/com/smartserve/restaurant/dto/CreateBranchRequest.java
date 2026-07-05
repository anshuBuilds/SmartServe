package com.smartserve.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(
        @NotBlank(message = "Branch name is required")
        @Size(max = 120, message = "Branch name must not exceed 120 characters")
        String name,

        @NotBlank(message = "Address is required")
        @Size(max = 300, message = "Address must not exceed 300 characters")
        String address,

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^[0-9+() -]{7,20}$",
                message = "Phone number must be 7 to 20 characters and contain only digits, spaces, +, - or parentheses"
        )
        String phone
) {
}
