package com.smartserve.analytics.dto;

import java.math.BigDecimal;

public record TablePerformanceRow(
        Integer tableNumber,
        Long servedOrderCount,
        BigDecimal revenue
) {
}

