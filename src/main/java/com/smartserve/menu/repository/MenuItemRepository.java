package com.smartserve.menu.repository;

import com.smartserve.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);

    List<MenuItem> findByCategoryId(Long categoryId);

    List<MenuItem> findByAvailableTrue();

    List<MenuItem> findByCategoryIdAndAvailableTrue(Long categoryId);
}
