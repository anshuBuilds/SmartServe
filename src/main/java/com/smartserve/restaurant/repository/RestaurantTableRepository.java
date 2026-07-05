package com.smartserve.restaurant.repository;

import com.smartserve.restaurant.entity.RestaurantTable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTableRepository
        extends JpaRepository<RestaurantTable, Long> {

    boolean existsByBranchIdAndTableNumberIgnoreCase(
            Long branchId,
            String tableNumber
    );

    List<RestaurantTable> findByBranchIdOrderByTableNumberAsc(
            Long branchId
    );

    Optional<RestaurantTable> findByIdAndBranchId(
            Long tableId,
            Long branchId
    );
}