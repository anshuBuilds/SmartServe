package com.smartserve.guest.dto;

import com.smartserve.menu.dto.MenuCategoryResponse;
import com.smartserve.menu.dto.MenuItemResponse;
import java.util.List;

public record GuestMenuResponse(List<MenuCategoryResponse> categories, List<MenuItemResponse> items) {}
