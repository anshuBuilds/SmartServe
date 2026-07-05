package com.smartserve.restaurant.repository;

import com.smartserve.restaurant.entity.Branch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository
        extends JpaRepository<Branch, Long> {

    boolean existsByRestaurantIdAndNameIgnoreCase(
            Long restaurantId,
            String name
    );

    List<Branch> findByRestaurantIdOrderByNameAsc(
            Long restaurantId
    );
}