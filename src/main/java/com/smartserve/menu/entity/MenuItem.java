package com.smartserve.menu.entity;

import com.smartserve.common.entity.BaseEntity;
import com.smartserve.menu.enums.FoodType;
import com.smartserve.menu.enums.SpiceLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@Entity
@Table(name = "menu_items")
public class MenuItem extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 150)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @Column(nullable = false)
    private Boolean available = true;

    @Column(nullable = false)
    private Integer preparationTimeMinutes;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FoodType foodType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SpiceLevel spiceLevel;
}
