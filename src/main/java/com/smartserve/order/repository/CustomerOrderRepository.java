package com.smartserve.order.repository;

import com.smartserve.analytics.dto.OrdersByStatusResponse;
import com.smartserve.analytics.dto.TablePerformanceRow;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByOrderStatus(OrderStatus orderStatus);
    List<CustomerOrder> findByTableNumber(Integer tableNumber);
    List<CustomerOrder> findByTableNumberAndOrderStatus(Integer tableNumber, OrderStatus orderStatus);

    @Query("""
            select coalesce(sum(o.totalAmount), 0)
            from CustomerOrder o
            where o.orderStatus = :status
                and o.createdAt >= :from
                and o.createdAt < :to
""")
    BigDecimal sumTotalAmountByStatusAndDateRange(
            @Param("status") OrderStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
        select count(o)
        from CustomerOrder o
        where o.orderStatus = :status
          and o.createdAt >= :from
          and o.createdAt < :to
        """)
    Long countByStatusAndDateRange(
            @Param("status") OrderStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
        select count(o)
        from CustomerOrder o
        where o.createdAt >= :from
          and o.createdAt < :to
        """)
    Long countAllByDateRange(
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
        select new com.smartserve.analytics.dto.OrdersByStatusResponse(
            o.orderStatus,
            count(o),
            sum(o.totalAmount)
        )
        from CustomerOrder o
        where o.createdAt >= :from
          and o.createdAt < :to
        group by o.orderStatus
        """)
    List<OrdersByStatusResponse> countOrdersGroupedByStatus(
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
    select new com.smartserve.analytics.dto.TablePerformanceRow(
        o.tableNumber,
        count(o),
        sum(o.totalAmount)
    )
    from CustomerOrder o
    where o.orderStatus = :status
      and o.createdAt >= :from
      and o.createdAt < :to
    group by o.tableNumber
    order by sum(o.totalAmount) desc, o.tableNumber asc
    """)
    List<TablePerformanceRow> findTablePerformanceRaw(
            @Param("status") OrderStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
