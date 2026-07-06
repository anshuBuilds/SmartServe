package com.smartserve.analytics.dto;

import java.math.BigDecimal;

public record TablePerformanceResponse(
        String tableNumber,
        Long servedOrderCount,
        BigDecimal revenue,
        BigDecimal averageOrderValue
) {
}