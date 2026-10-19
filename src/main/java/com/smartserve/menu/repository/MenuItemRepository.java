package com.smartserve.menu.repository;

import com.smartserve.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);

    boolean existsByCategoryId(Long categoryId);

    List<MenuItem> findByCategoryId(Long categoryId);

    List<MenuItem> findByCategoryIdAndAvailableTrue(Long categoryId);

    Optional<MenuItem> findByIdAndCategoryRestaurantId(Long id, Long restaurantId);

    List<MenuItem> findByCategoryRestaurantId(Long restaurantId);

    List<MenuItem> findByCategoryRestaurantIdAndAvailableTrue(Long restaurantId);
}
