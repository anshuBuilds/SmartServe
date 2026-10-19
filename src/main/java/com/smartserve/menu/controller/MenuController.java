package com.smartserve.menu.controller;

import com.smartserve.common.response.ApiResponse;
import com.smartserve.common.security.MenuAccessService;
import com.smartserve.menu.dto.CreateMenuCategoryRequest;
import com.smartserve.menu.dto.CreateMenuItemRequest;
import com.smartserve.menu.dto.MenuCategoryResponse;
import com.smartserve.menu.dto.MenuItemResponse;
import com.smartserve.menu.dto.UpdateMenuCategoryRequest;
import com.smartserve.menu.dto.UpdateMenuItemRequest;
import com.smartserve.menu.service.MenuService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Every endpoint works on one restaurant's menu. Administrators pass {@code restaurantId};
 * other staff may omit it and get their own restaurant.
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final MenuAccessService menuAccessService;

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> createCategory(
            @RequestParam(required = false) Long restaurantId,
            @Valid @RequestBody CreateMenuCategoryRequest request,
            Authentication authentication
    ) {
        MenuCategoryResponse category = menuService.createCategory(restaurant(authentication, restaurantId), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu category created", category));
    }

    @GetMapping("/categories")
    public ApiResponse<List<MenuCategoryResponse>> getCategories(
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            Authentication authentication
    ) {
        Long id = restaurant(authentication, restaurantId);
        List<MenuCategoryResponse> categories = activeOnly
                ? menuService.getActiveCategories(id)
                : menuService.getAllCategories(id);
        return ApiResponse.success(categories);
    }

    @GetMapping("/categories/{categoryId}")
    public ApiResponse<MenuCategoryResponse> getCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Long restaurantId,
            Authentication authentication
    ) {
        return ApiResponse.success(menuService.getCategory(restaurant(authentication, restaurantId), categoryId));
    }

    @PutMapping("/categories/{categoryId}")
    public ApiResponse<MenuCategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Long restaurantId,
            @Valid @RequestBody UpdateMenuCategoryRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success("Menu category updated",
                menuService.updateCategory(restaurant(authentication, restaurantId), categoryId, request));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Long restaurantId,
            Authentication authentication
    ) {
        menuService.deleteCategory(restaurant(authentication, restaurantId), categoryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createItem(
            @RequestParam(required = false) Long restaurantId,
            @Valid @RequestBody CreateMenuItemRequest request,
            Authentication authentication
    ) {
        MenuItemResponse item = menuService.createItem(restaurant(authentication, restaurantId), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item created", item));
    }

    @GetMapping("/items")
    public ApiResponse<List<MenuItemResponse>> getItems(
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            Authentication authentication
    ) {
        Long id = restaurant(authentication, restaurantId);
        List<MenuItemResponse> items;
        if (categoryId != null) {
            items = menuService.getItemsByCategory(id, categoryId, availableOnly);
        } else {
            items = availableOnly ? menuService.getAvailableItems(id) : menuService.getAllItems(id);
        }
        return ApiResponse.success(items);
    }

    @GetMapping("/items/{itemId}")
    public ApiResponse<MenuItemResponse> getItem(
            @PathVariable Long itemId,
            @RequestParam(required = false) Long restaurantId,
            Authentication authentication
    ) {
        return ApiResponse.success(menuService.getItem(restaurant(authentication, restaurantId), itemId));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<MenuItemResponse> updateItem(
            @PathVariable Long itemId,
            @RequestParam(required = false) Long restaurantId,
            @Valid @RequestBody UpdateMenuItemRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success("Menu item updated",
                menuService.updateItem(restaurant(authentication, restaurantId), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long itemId,
            @RequestParam(required = false) Long restaurantId,
            Authentication authentication
    ) {
        menuService.deleteItem(restaurant(authentication, restaurantId), itemId);
        return ResponseEntity.noContent().build();
    }

    private Long restaurant(Authentication authentication, Long requestedRestaurantId) {
        return menuAccessService.resolveRestaurant(authentication, requestedRestaurantId);
    }
}
