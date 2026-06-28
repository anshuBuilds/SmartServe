package com.smartserve.order.repository;

import com.smartserve.analytics.dto.TopMenuItemRow;
import com.smartserve.order.entity.OrderItem;
import com.smartserve.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {


    @Query("""
        select new com.smartserve.analytics.dto.TopMenuItemRow(
            i.menuItem.id,
            i.menuItem.name,
            sum(i.quantity),
            sum(i.lineTotal),
            count(distinct i.order.id)
        )
        from OrderItem i
        where i.order.orderStatus = :status
          and i.order.createdAt >= :from
          and i.order.createdAt < :to
        group by i.menuItem.id, i.menuItem.name
        order by sum(i.quantity) desc, sum(i.lineTotal) desc
        """)
    List<TopMenuItemRow> findTopSellingItems(
            @Param("status") OrderStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
