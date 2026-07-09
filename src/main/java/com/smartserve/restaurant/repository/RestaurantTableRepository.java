package com.smartserve.restaurant.repository;

import com.smartserve.restaurant.entity.RestaurantTable;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

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
    Optional<RestaurantTable> findByQrToken(String qrToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RestaurantTable> findForUpdateByIdAndBranchId(
            Long tableId,
            Long branchId
    );
}
