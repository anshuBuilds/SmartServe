package com.smartserve.analytics.dto;

import java.math.BigDecimal;

public record TablePerformanceResponse(
        Integer tableNumber,
        Long servedOrderCount,
        BigDecimal revenue,
        BigDecimal averageOrderValue
) {
}