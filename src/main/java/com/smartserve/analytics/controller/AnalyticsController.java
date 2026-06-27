package com.smartserve.analytics.controller;

import com.smartserve.analytics.dto.OrdersByStatusResponse;
import com.smartserve.analytics.dto.SalesSummaryResponse;
import com.smartserve.analytics.dto.TablePerformanceResponse;
import com.smartserve.analytics.dto.TopMenuItemResponse;
import com.smartserve.analytics.service.AnalyticsService;
import com.smartserve.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/sales-summary")
    public ApiResponse<SalesSummaryResponse> getSalesSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {
        return ApiResponse.success(
                analyticsService.getSalesSummary(from, to)
        );
    }

    @GetMapping("/top-items")
    public ApiResponse<List<TopMenuItemResponse>> getTopItems(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,

            @RequestParam(required = false)
            Integer limit
    ) {
        return ApiResponse.success(
                analyticsService.getTopSellingItems(from, to, limit)
        );
    }

    @GetMapping("/orders-by-status")
    public ApiResponse<List<OrdersByStatusResponse>> getOrdersByStatus(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {
        return ApiResponse.success(
                analyticsService.getOrdersByStatus(from, to)
        );
    }

    @GetMapping("/table-performance")
    public ApiResponse<List<TablePerformanceResponse>> getTablePerformance(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {
        return ApiResponse.success(
                analyticsService.getTablePerformance(from, to)
        );
    }
}