package com.smartserve.menu.dto;

import com.smartserve.menu.enums.FoodType;
import com.smartserve.menu.enums.SpiceLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class MenuItemResponse {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Long categoryId;

    private String categoryName;

    private Boolean available;

    private Integer preparationTimeMinutes;

    private String imageUrl;

    private FoodType foodType;

    private SpiceLevel spiceLevel;

    private Instant createdAt;

    private Instant updatedAt;
}