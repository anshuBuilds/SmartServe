package com.smartserve.menu.dto;

import com.smartserve.menu.enums.FoodType;
import com.smartserve.menu.enums.SpiceLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateMenuItemRequest {

    @NotBlank(message = "Item name is required")
    @Size(max = 150, message = "Item name must be at most 150 characters")
    private String name;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Category id is required")
    private Long categoryId;

    private Boolean available;

    @NotNull(message = "Preparation time is required")
    @Min(value = 1, message = "Preparation time must be at least 1 minute")
    private Integer preparationTimeMinutes;

    @Size(max = 500, message = "Image URL must be at most 500 characters")
    private String imageUrl;

    @NotNull(message = "Food type is required")
    private FoodType foodType;

    @NotNull(message = "Spice level is required")
    private SpiceLevel spiceLevel;
}