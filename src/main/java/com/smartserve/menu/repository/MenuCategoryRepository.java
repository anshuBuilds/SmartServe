package com.smartserve.menu.repository;

import com.smartserve.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    Optional<MenuCategory> findByIdAndRestaurantId(Long id, Long restaurantId);

    Optional<MenuCategory> findByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);

    boolean existsByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);

    List<MenuCategory> findByRestaurantIdOrderByDisplayOrderAsc(Long restaurantId);

    List<MenuCategory> findByRestaurantIdAndActiveTrueOrderByDisplayOrderAsc(Long restaurantId);

    boolean existsByRestaurantIdAndDisplayOrderAndIdNot(Long restaurantId, Integer displayOrder, Long id);

    boolean existsByRestaurantIdAndDisplayOrder(Long restaurantId, Integer displayOrder);

    @Query("select coalesce(max(c.displayOrder), 0) from MenuCategory c where c.restaurant.id = :restaurantId")
    Integer findMaxDisplayOrder(Long restaurantId);
}
