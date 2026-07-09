package com.smartserve.restaurant.entity;

import com.smartserve.common.entity.BaseEntity;
import com.smartserve.restaurant.enums.TableStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(
        name = "restaurant_tables",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_branch_table_number",
                        columnNames = {"branch_id", "table_number"}
                )
        }
)
@Getter
@Setter
public class RestaurantTable extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "table_number", nullable = false, length = 20)
    private String tableNumber;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(name = "qr_token", unique = true, length = 64)
    private String qrToken;

    @PrePersist
    void ensureQrToken() {
        if (qrToken == null) qrToken = UUID.randomUUID().toString().replace("-", "");
    }
}
