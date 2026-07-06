package com.smartserve.analytics.dto;

import java.math.BigDecimal;

public record TablePerformanceRow(
        String tableNumber,
        Long servedOrderCount,
        BigDecimal revenue
) {
}

