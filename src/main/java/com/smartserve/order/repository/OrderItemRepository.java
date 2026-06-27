package com.smartserve.order.repository;

import com.smartserve.analytics.dto.TopMenuItemResponse;
import com.smartserve.order.entity.OrderItem;
import com.smartserve.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
        select new com.smartserve.analytics.dto.TopMenuItemResponse(
            oi.menuItem.id,
            oi.itemName,
            sum(oi.quantity),
            sum(oi.lineTotal),
            count(distinct oi.order.id)
        )
        from OrderItem oi
        where oi.o                                                                                                                                  rder.orderStatus = :status
          and (:from is null or oi.order.createdAt >= :from)
          and (:to is null or oi.order.createdAt < :to)
        group by oi.menuItem.id, oi.itemName
        order by sum(oi.quantity) desc, sum(oi.lineTotal) desc, oi.itemName asc
        """)
    List<TopMenuItemResponse> findTopMenuItems(
            @Param("status") OrderStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
