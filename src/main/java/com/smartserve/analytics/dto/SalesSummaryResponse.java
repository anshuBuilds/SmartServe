package com.smartserve.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SalesSummaryResponse(
        BigDecimal totalRevenue,
        Long servedOrderCount,
        Long totalOrderCount,
        Long cancelledOrderCount,
        BigDecimal averageOrderValue,
        Instant from,
        Instant to
) {
}