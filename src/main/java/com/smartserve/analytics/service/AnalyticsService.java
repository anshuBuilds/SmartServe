package com.smartserve.analytics.service;

import com.smartserve.analytics.dto.OrdersByStatusResponse;
import com.smartserve.analytics.dto.SalesSummaryResponse;
import com.smartserve.analytics.dto.TopMenuItemResponse;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.order.repository.OrderItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public AnalyticsService(CustomerOrderRepository customerOrderRepository, OrderItemRepository orderItemRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public SalesSummaryResponse getSalesSummary(
            Instant from,
            Instant to
    ) {
        validateDateRange(from, to);

        BigDecimal totalRevenue =
                customerOrderRepository.sumTotalAmountByStatusAndDateRange(
                        OrderStatus.SERVED,
                        from,
                        to
                );

        long servedOrderCount =
                customerOrderRepository.countByStatusAndDateRange(
                        OrderStatus.SERVED,
                        from,
                        to
                );

        long totalOrderCount =
                customerOrderRepository.countAllByDateRange(from, to);

        long cancelledOrderCount =
                customerOrderRepository.countByStatusAndDateRange(
                        OrderStatus.CANCELLED,
                        from,
                        to
                );

        BigDecimal averageOrderValue = calculateAverage(
                totalRevenue,
                servedOrderCount
        );

        return new SalesSummaryResponse(
                normalize(totalRevenue),
                servedOrderCount,
                totalOrderCount,
                cancelledOrderCount,
                averageOrderValue,
                from,
                to
        );
    }

    public List<TopMenuItemResponse> getTopSellingItems(
            Instant from,
            Instant to,
            Integer limit
    ) {
        validateDateRange(from, to);

        int normalizedLimit = validateLimit(limit);

        return orderItemRepository.findTopSellingItems(
                OrderStatus.SERVED,
                from,
                to,
                PageRequest.of(0, normalizedLimit)
        )
                .stream()
                .map(row -> new TopMenuItemResponse(
                        row.menuItemId(),
                        row.itemName(),
                        row.quantitySold(),
                        normalize(row.revenue()),
                        row.orderCount()
                ))
                .toList();
    }

    public List<OrdersByStatusResponse> getOrdersByStatus(
            Instant from,
            Instant to
    ) {
        validateDateRange(from, to);

        List<OrdersByStatusResponse> databaseResults =
                customerOrderRepository.countOrdersGroupedByStatus(from, to);

        Map<OrderStatus, OrdersByStatusResponse> resultByStatus =
                new EnumMap<>(OrderStatus.class);

        for(OrdersByStatusResponse result : databaseResults) {
            resultByStatus.put(
                    result.status(),
                    new OrdersByStatusResponse(
                            result.status(),
                            result.orderCount(),
                            normalize(result.totalAmount())
                    )
            );
        }

        return Arrays.stream(OrderStatus.values())
                .map(status -> resultByStatus.getOrDefault(
                        status,
                        new OrdersByStatusResponse(
                                status,
                                0L,
                                BigDecimal.ZERO
                        )
                ))
                .toList();
    }

    public List<TablePerformanceResponse> getTablePerformance(
            Instant from,
            Instant to
    ) {

    }

    private void validateDateRange(Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException(
                    "'from' must be earlier than 'to'"
            );
        }
    }

    private BigDecimal calculateAverage(
            BigDecimal total,
            long count
    ) {
        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return normalize(total).divide(
                BigDecimal.valueOf(count),
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int validateLimit(Integer limit) {
        if (limit == null) {
            return 5;
        }

        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException(
                    "'limit' must be between 1 and 50"
            );
        }

        return limit;
    }

}