package com.smartserve.menu.controller;

import com.smartserve.common.response.ApiResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> createCategory(
            @Valid @RequestBody CreateMenuCategoryRequest request
    ) {
        MenuCategoryResponse category = menuService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu category created", category));
    }

    @GetMapping("/categories")
    public ApiResponse<List<MenuCategoryResponse>> getCategories(
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        List<MenuCategoryResponse> categories = activeOnly
                ? menuService.getActiveCategories()
                : menuService.getAllCategories();
        return ApiResponse.success(categories);
    }

    @GetMapping("/categories/{categoryId}")
    public ApiResponse<MenuCategoryResponse> getCategory(@PathVariable Long categoryId) {
        return ApiResponse.success(menuService.getCategory(categoryId));
    }

    @PutMapping("/categories/{categoryId}")
    public ApiResponse<MenuCategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateMenuCategoryRequest request
    ) {
        return ApiResponse.success("Menu category updated", menuService.updateCategory(categoryId, request));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        menuService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createItem(
            @Valid @RequestBody CreateMenuItemRequest request
    ) {
        MenuItemResponse item = menuService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item created", item));
    }

    @GetMapping("/items")
    public ApiResponse<List<MenuItemResponse>> getItems(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "false") boolean availableOnly
    ) {
        List<MenuItemResponse> items = categoryId == null
                ? getItemsWithoutCategoryFilter(availableOnly)
                : menuService.getItemsByCategory(categoryId, availableOnly);
        return ApiResponse.success(items);
    }

    @GetMapping("/items/{itemId}")
    public ApiResponse<MenuItemResponse> getItem(@PathVariable Long itemId) {
        return ApiResponse.success(menuService.getItem(itemId));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<MenuItemResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateMenuItemRequest request
    ) {
        return ApiResponse.success("Menu item updated", menuService.updateItem(itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        menuService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }

    private List<MenuItemResponse> getItemsWithoutCategoryFilter(boolean availableOnly) {
        return availableOnly ? menuService.getAvailableItems() : menuService.getAllItems();
    }
}
