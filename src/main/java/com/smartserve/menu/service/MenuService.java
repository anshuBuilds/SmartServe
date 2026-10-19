package com.smartserve.menu.service;

import com.smartserve.common.exception.BadRequestException;
import com.smartserve.common.exception.ConflictException;
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
import com.smartserve.restaurant.repository.RestaurantRepository;
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
    private final RestaurantRepository restaurantRepository;

    public MenuCategoryResponse createCategory(Long restaurantId, CreateMenuCategoryRequest request) {
        String name = request.getCategoryName().trim();

        if (menuCategoryRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, name)) {
            throw new BadRequestException("Category already exists");
        }

        Integer displayOrder = resolveDisplayOrder(restaurantId, request.getDisplayOrder());

        MenuCategory category = new MenuCategory();
        category.setRestaurant(restaurantRepository.getReferenceById(restaurantId));
        category.setName(name);
        category.setDescription(request.getDescription());
        category.setDisplayOrder(displayOrder);

        MenuCategory savedCategory = menuCategoryRepository.save(category);

        return toCategoryResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getAllCategories(Long restaurantId) {
        return menuCategoryRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId).stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getActiveCategories(Long restaurantId) {
        return menuCategoryRepository.findByRestaurantIdAndActiveTrueOrderByDisplayOrderAsc(restaurantId).stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuCategoryResponse getCategory(Long restaurantId, Long categoryId) {
        return toCategoryResponse(findCategory(restaurantId, categoryId));
    }

    public MenuCategoryResponse updateCategory(Long restaurantId, Long categoryId, UpdateMenuCategoryRequest request) {
        MenuCategory category = findCategory(restaurantId, categoryId);

        String name = request.getName().trim();

        menuCategoryRepository.findByRestaurantIdAndNameIgnoreCase(restaurantId, name)
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Menu category already exists");
                });

        category.setName(name);
        category.setDescription(request.getDescription());

        if (request.getDisplayOrder() != null) {
            validateDisplayOrderIsAvailable(restaurantId, request.getDisplayOrder(), categoryId);
            category.setDisplayOrder(request.getDisplayOrder());
        }

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        return toCategoryResponse(category);
    }

    public void deleteCategory(Long restaurantId, Long categoryId) {
        MenuCategory category = findCategory(restaurantId, categoryId);

        if (menuItemRepository.existsByCategoryId(categoryId)) {
            throw new BadRequestException("Cannot delete a category that has menu items");
        }
        menuCategoryRepository.delete(category);
    }

    public MenuItemResponse createItem(Long restaurantId, CreateMenuItemRequest request) {
        MenuCategory category = findCategory(restaurantId, request.getCategoryId());
        rejectDuplicateItemName(category.getId(), request.getName().trim(), null);

        MenuItem item = new MenuItem();
        applyItemRequest(item, category, request);
        item.setAvailable(true);

        MenuItem savedItem = menuItemRepository.save(item);

        return toItemResponse(savedItem);
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAllItems(Long restaurantId) {
        return menuItemRepository.findByCategoryRestaurantId(restaurantId).stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAvailableItems(Long restaurantId) {
        return menuItemRepository.findByCategoryRestaurantIdAndAvailableTrue(restaurantId).stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getItemsByCategory(Long restaurantId, Long categoryId, Boolean availableOnly) {
        findCategory(restaurantId, categoryId);

        List<MenuItem> items = Boolean.TRUE.equals(availableOnly)
                ? menuItemRepository.findByCategoryIdAndAvailableTrue(categoryId)
                : menuItemRepository.findByCategoryId(categoryId);

        return items.stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getItem(Long restaurantId, Long itemId) {
        return toItemResponse(findItem(restaurantId, itemId));
    }

    public MenuItemResponse updateItem(Long restaurantId, Long itemId, UpdateMenuItemRequest request) {
        MenuItem item = findItem(restaurantId, itemId);
        MenuCategory category = findCategory(restaurantId, request.getCategoryId());
        String requestedName = request.getName().trim();
        boolean duplicateIdentityChanged = !item.getCategory().getId().equals(category.getId())
                || !item.getName().trim().equalsIgnoreCase(requestedName);
        if (duplicateIdentityChanged) {
            rejectDuplicateItemName(category.getId(), requestedName, itemId);
        }

        applyItemRequest(item, category, request);

        if (request.getAvailable() != null) {
            item.setAvailable(request.getAvailable());
        }

        return toItemResponse(item);
    }

    private void rejectDuplicateItemName(Long categoryId, String name, Long currentItemId) {
        boolean duplicate = currentItemId == null
                ? menuItemRepository.existsByCategoryIdAndNameIgnoreCase(categoryId, name)
                : menuItemRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(categoryId, name, currentItemId);

        if (duplicate) {
            throw new ConflictException("A menu item with this name already exists in the category");
        }
    }

    public void deleteItem(Long restaurantId, Long itemId) {
        menuItemRepository.delete(findItem(restaurantId, itemId));
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
                category.getRestaurant().getId(),
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

    // A category or item of another restaurant is reported as not found, not forbidden,
    // so ids from other restaurants reveal nothing.
    private MenuCategory findCategory(Long restaurantId, Long categoryId) {
        return menuCategoryRepository.findByIdAndRestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found"));
    }

    private MenuItem findItem(Long restaurantId, Long itemId) {
        return menuItemRepository.findByIdAndCategoryRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
    }

    private Integer resolveDisplayOrder(Long restaurantId, Integer requestedDisplayOrder) {
        if (requestedDisplayOrder == null) {
            return menuCategoryRepository.findMaxDisplayOrder(restaurantId) + 1;
        }

        validateDisplayOrderIsAvailable(restaurantId, requestedDisplayOrder, null);
        return requestedDisplayOrder;
    }

    private void validateDisplayOrderIsAvailable(Long restaurantId, Integer displayOrder, Long currentCategoryId) {
        boolean displayOrderTaken = currentCategoryId == null
                ? menuCategoryRepository.existsByRestaurantIdAndDisplayOrder(restaurantId, displayOrder)
                : menuCategoryRepository.existsByRestaurantIdAndDisplayOrderAndIdNot(
                        restaurantId, displayOrder, currentCategoryId);

        if (displayOrderTaken) {
            throw new BadRequestException("Display order already exists");
        }
    }
}
