package com.smartserve.menu.service;

import com.smartserve.common.exception.BadRequestException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.menu.dto.CreateMenuCategoryRequest;
import com.smartserve.menu.dto.CreateMenuItemRequest;
import com.smartserve.menu.dto.MenuCategoryResponse;
import com.smartserve.menu.dto.MenuItemResponse;
import com.smartserve.menu.dto.UpdateMenuCategoryRequest;
import com.smartserve.menu.dto.UpdateMenuItemRequest;
import com.smartserve.menu.entity.MenuCategory;
import com.smartserve.menu.entity.MenuItem;
import com.smartserve.menu.repository.MenuCategoryRepository;
import com.smartserve.menu.repository.MenuItemRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuService {

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    public MenuCategoryResponse createCategory(CreateMenuCategoryRequest request) {
        String name = request.getCategoryName().trim();

        if (menuCategoryRepository.existsByName(name)) {
            throw new BadRequestException("Category already exists");
        }

        Integer displayOrder = resolveDisplayOrder(request.getDisplayOrder());

        MenuCategory category = new MenuCategory();
        category.setName(name);
        category.setDescription(request.getDescription());
        category.setDisplayOrder(displayOrder);

        MenuCategory savedCategory = menuCategoryRepository.save(category);

        return toCategoryResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getAllCategories() {
        return menuCategoryRepository.findAll().stream()
                .sorted(Comparator.comparing(MenuCategory::getDisplayOrder))
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getActiveCategories() {
        return menuCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuCategoryResponse getCategory(Long categoryId) {
        return toCategoryResponse(findCategory(categoryId));
    }

    public MenuCategoryResponse updateCategory(Long categoryId, UpdateMenuCategoryRequest request) {
        MenuCategory category = findCategory(categoryId);

        String name = request.getName().trim();

        menuCategoryRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Menu category already exists");
                });

        category.setName(name);
        category.setDescription(request.getDescription());

        if (request.getDisplayOrder() != null) {
            validateDisplayOrderIsAvailable(request.getDisplayOrder(), categoryId);
            category.setDisplayOrder(request.getDisplayOrder());
        }

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        return toCategoryResponse(category);
    }

    public void deleteCategory(Long categoryId) {
        MenuCategory category = findCategory(categoryId);

        if (!menuItemRepository.findByCategoryId(categoryId).isEmpty()) {
            throw new BadRequestException("Cannot delete a category that has menu items");
        }
        menuCategoryRepository.delete(category);
    }

    public MenuItemResponse createItem(CreateMenuItemRequest request) {
        MenuCategory category = findCategory(request.getCategoryId());

        MenuItem item = new MenuItem();
        applyItemRequest(item, category, request);
        item.setAvailable(true);

        MenuItem savedItem = menuItemRepository.save(item);

        return toItemResponse(savedItem);
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllItems() {
        return menuItemRepository.findAll().stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAvailableItems() {
        return menuItemRepository.findByAvailableTrue().stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getItemsByCategory(Long categoryId, Boolean availableOnly) {
        findCategory(categoryId);

        List<MenuItem> items = Boolean.TRUE.equals(availableOnly)
                ? menuItemRepository.findByCategoryIdAndAvailableTrue(categoryId)
                : menuItemRepository.findByCategoryId(categoryId);

        return items.stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getItem(Long itemId) {
        return toItemResponse(findItem(itemId));
    }

    public MenuItemResponse updateItem(Long itemId, UpdateMenuItemRequest request) {
        MenuItem item = findItem(itemId);
        MenuCategory category = findCategory(request.getCategoryId());

        applyItemRequest(item, category, request);

        if (request.getAvailable() != null) {
            item.setAvailable(request.getAvailable());
        }

        return toItemResponse(item);
    }

    public void deleteItem(Long itemId) {
        menuItemRepository.delete(findItem(itemId));
    }

    private void applyItemRequest(MenuItem item, MenuCategory category, CreateMenuItemRequest request) {
        item.setName(request.getName().trim());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(category);
        item.setPreparationTimeMinutes(request.getPreparationTimeMinutes());
        item.setImageUrl(request.getImageUrl());
        item.setFoodType(request.getFoodType());
        item.setSpiceLevel(request.getSpiceLevel());
    }

    private void applyItemRequest(MenuItem item, MenuCategory category, UpdateMenuItemRequest request) {
        item.setName(request.getName().trim());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(category);
        item.setPreparationTimeMinutes(request.getPreparationTimeMinutes());
        item.setImageUrl(request.getImageUrl());
        item.setFoodType(request.getFoodType());
        item.setSpiceLevel(request.getSpiceLevel());
    }

    private MenuCategoryResponse toCategoryResponse(MenuCategory category) {
        return new MenuCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.getActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private MenuItemResponse toItemResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getAvailable(),
                item.getPreparationTimeMinutes(),
                item.getImageUrl(),
                item.getFoodType(),
                item.getSpiceLevel(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private MenuCategory findCategory(Long categoryId) {
        return menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found"));
    }

    private MenuItem findItem(Long itemId) {
        return menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
    }

    private Integer resolveDisplayOrder(Integer requestedDisplayOrder) {
        if (requestedDisplayOrder == null) {
            return menuCategoryRepository.findMaxDisplayOrder() + 1;
        }

        validateDisplayOrderIsAvailable(requestedDisplayOrder, null);
        return requestedDisplayOrder;
    }

    private void validateDisplayOrderIsAvailable(Integer displayOrder, Long currentCategoryId) {
        boolean displayOrderTaken = menuCategoryRepository.findAll().stream()
                .anyMatch(category -> category.getDisplayOrder().equals(displayOrder)
                        && !category.getId().equals(currentCategoryId));

        if (displayOrderTaken) {
            throw new BadRequestException("Display order already exists");
        }
    }
}
