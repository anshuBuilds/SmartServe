package com.smartserve.analytics.dto;

import java.math.BigDecimal;

public record TopMenuItemRow(
        Long menuItemId,
        String itemName,
        Long quantitySold,
        BigDecimal revenue,
        Long orderCount
) {
}