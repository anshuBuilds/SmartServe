package com.smartserve.order.repository;

import com.smartserve.analytics.dto.OrdersByStatusResponse;
import com.smartserve.analytics.dto.TablePerformanceRow;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByOrderStatus(OrderStatus orderStatus);
    List<CustomerOrder> findByTableId(Long tableId);
    List<CustomerOrder> findByTableIdAndOrderStatus(Long tableId, OrderStatus orderStatus);
    List<CustomerOrder> findByBranchId(Long branchId);
    List<CustomerOrder> findByBranchIdAndOrderStatus(Long branchId, OrderStatus orderStatus);
    Optional<CustomerOrder> findByTrackingToken(String trackingToken);

    @EntityGraph(attributePaths = {"branch", "table", "items", "items.menuItem"})
    @Query("""
            select o from CustomerOrder o
            where o.branch.id = :branchId and o.orderStatus in :statuses
                         and (
                                      o.paymentStatus = com.smartserve.order.enums.PaymentStatus.NOT_REQUIRED
                                      or o.paymentStatus = com.smartserve.order.enums.PaymentStatus.PAID
                                  )
            order by case o.orderStatus
                when com.smartserve.order.enums.OrderStatus.PENDING then 0
                when com.smartserve.order.enums.OrderStatus.PREPARING then 1
                when com.smartserve.order.enums.OrderStatus.READY then 2
                else 3 end,
                o.createdAt asc
            """)
    List<CustomerOrder> findKitchenQueue(@Param("branchId") Long branchId,
                                          @Param("statuses") Collection<OrderStatus> statuses);

    @EntityGraph(attributePaths = {"branch", "table", "items", "items.menuItem"})
    @Query("select o from CustomerOrder o where o.id = :orderId")
    Optional<CustomerOrder> findDetailedById(@Param("orderId") Long orderId);

    @Query("""
            select o.id from CustomerOrder o
            where o.branch.id = :branchId
              and o.orderStatus in :statuses
              and o.createdAt >= :from and o.createdAt < :to
            order by o.createdAt desc
            """)
    Page<Long> findKitchenHistoryIds(@Param("branchId") Long branchId,
                                     @Param("statuses") Collection<OrderStatus> statuses,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     Pageable pageable);

    @EntityGraph(attributePaths = {"branch", "table", "items", "items.menuItem"})
    @Query("select o from CustomerOrder o where o.id in :ids")
    List<CustomerOrder> findDetailedByIdIn(@Param("ids") Collection<Long> ids);

    @Query("""
            select coalesce(sum(o.totalAmount), 0) from CustomerOrder o
            where o.orderStatus = :status and o.createdAt >= :from and o.createdAt < :to
            """)
    BigDecimal sumTotalAmountByStatusAndDateRange(@Param("status") OrderStatus status,
                                                   @Param("from") Instant from,
                                                   @Param("to") Instant to);

    @Query("""
            select count(o) from CustomerOrder o
            where o.orderStatus = :status and o.createdAt >= :from and o.createdAt < :to
            """)
    Long countByStatusAndDateRange(@Param("status") OrderStatus status,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);

    @Query("select count(o) from CustomerOrder o where o.createdAt >= :from and o.createdAt < :to")
    Long countAllByDateRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select new com.smartserve.analytics.dto.OrdersByStatusResponse(
                o.orderStatus, count(o), sum(o.totalAmount))
            from CustomerOrder o
            where o.createdAt >= :from and o.createdAt < :to
            group by o.orderStatus
            """)
    List<OrdersByStatusResponse> countOrdersGroupedByStatus(@Param("from") Instant from,
                                                             @Param("to") Instant to);

    @Query("""
            select new com.smartserve.analytics.dto.TablePerformanceRow(
                o.table.tableNumber, count(o), sum(o.totalAmount))
            from CustomerOrder o
            where o.orderStatus = :status
              and o.table is not null
              and o.createdAt >= :from and o.createdAt < :to
            group by o.table.id, o.table.tableNumber
            order by sum(o.totalAmount) desc, o.table.tableNumber asc
            """)
    List<TablePerformanceRow> findTablePerformanceRaw(@Param("status") OrderStatus status,
                                                        @Param("from") Instant from,
                                                        @Param("to") Instant to);
}

