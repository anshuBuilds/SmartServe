package com.smartserve.kitchen.dto;

public record KitchenTicketItemResponse(
        Long orderItemId,
        Long menuItemId,
        String itemName,
        Integer quantity,
        Integer preparationTimeMinutes
) {}
