package com.smartserve.restaurant.repository;

import com.smartserve.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository
        extends JpaRepository<Restaurant, Long> {

    boolean existsByNameIgnoreCase(String name);
}