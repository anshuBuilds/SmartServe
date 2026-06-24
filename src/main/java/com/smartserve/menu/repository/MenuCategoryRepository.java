package com.smartserve.menu.repository;

import com.smartserve.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    Optional<MenuCategory> findByName(String name);

    boolean existsByName(String name);

    List<MenuCategory> findByActiveTrueOrderByDisplayOrderAsc();

    boolean existsByDisplayOrder(Integer displayOrder);

    @Query("Select coalesce(max(c.displayOrder), 0) from MenuCategory c")
    Integer findMaxDisplayOrder();
}
