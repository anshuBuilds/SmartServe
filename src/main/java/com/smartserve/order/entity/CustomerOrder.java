package com.smartserve.order.entity;

import com.smartserve.common.entity.BaseEntity;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.enums.OrderType;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.entity.RestaurantTable;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customer_orders", indexes = {
        @Index(name = "idx_orders_branch_status_created", columnList = "branch_id, order_status, created_at"),
        @Index(name = "idx_orders_branch_updated", columnList = "branch_id, updated_at")
})
public class CustomerOrder extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderType orderType = OrderType.DINE_IN;

    @Column(length = 20)
    private String customerPhone;

    @Column(nullable = false)
    private Boolean smsConsent = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable table;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String specialInstructions;

    private Instant preparationStartedAt;
    private Instant readyAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
