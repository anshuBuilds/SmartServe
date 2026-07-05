package com.smartserve.restaurant.controller;

import com.smartserve.common.response.ApiResponse;
import com.smartserve.restaurant.dto.BranchResponse;
import com.smartserve.restaurant.dto.CreateBranchRequest;
import com.smartserve.restaurant.dto.CreateRestaurantRequest;
import com.smartserve.restaurant.dto.RestaurantResponse;
import com.smartserve.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    public ResponseEntity<ApiResponse<RestaurantResponse>> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest request
    ) {
        RestaurantResponse restaurant =
                restaurantService.createRestaurant(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Restaurant created",
                                restaurant
                        )
                );
    }

    @GetMapping
    public ApiResponse<List<RestaurantResponse>> getRestaurants() {
        return ApiResponse.success(
                restaurantService.getRestaurants()
        );
    }

    @GetMapping("/{restaurantId}")
    public ApiResponse<RestaurantResponse> getRestaurant(
            @PathVariable Long restaurantId
    ) {
        return ApiResponse.success(
                restaurantService.getRestaurant(restaurantId)
        );
    }

    @PostMapping("/{restaurantId}/branches")
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @PathVariable Long restaurantId,
            @Valid @RequestBody CreateBranchRequest request
    ) {
        BranchResponse branch =
                restaurantService.createBranch(
                        restaurantId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Branch created",
                                branch
                        )
                );
    }

    @GetMapping("/{restaurantId}/branches")
    public ApiResponse<List<BranchResponse>> getBranches(
            @PathVariable Long restaurantId
    ) {
        return ApiResponse.success(
                restaurantService.getBranches(restaurantId)
        );
    }
}