package com.smartserve.restaurant.controller;

import com.smartserve.common.response.ApiResponse;
import com.smartserve.restaurant.dto.BranchResponse;
import com.smartserve.restaurant.dto.CreateTableRequest;
import com.smartserve.restaurant.dto.TableResponse;
import com.smartserve.restaurant.dto.UpdateTableStatusRequest;
import com.smartserve.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final RestaurantService restaurantService;

    @GetMapping("/{branchId}")
    public ApiResponse<BranchResponse> getBranch(
            @PathVariable Long branchId
    ) {
        return ApiResponse.success(
                restaurantService.getBranch(branchId)
        );
    }

    @PostMapping("/{branchId}/tables")
    public ResponseEntity<ApiResponse<TableResponse>> createTable(
            @PathVariable Long branchId,
            @Valid @RequestBody CreateTableRequest request
    ) {
        TableResponse table =
                restaurantService.createTable(
                        branchId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Table created",
                                table
                        )
                );
    }

    @GetMapping("/{branchId}/tables")
    public ApiResponse<List<TableResponse>> getTables(
            @PathVariable Long branchId
    ) {
        return ApiResponse.success(
                restaurantService.getTables(branchId)
        );
    }

    @PatchMapping("/{branchId}/tables/{tableId}/status")
    public ApiResponse<TableResponse> updateTableStatus(
            @PathVariable Long branchId,
            @PathVariable Long tableId,
            @Valid @RequestBody UpdateTableStatusRequest request
    ) {
        TableResponse table =
                restaurantService.updateTableStatus(
                        branchId,
                        tableId,
                        request
                );

        return ApiResponse.success(
                "Table status updated",
                table
        );
    }

    @PostMapping("/{branchId}/tables/{tableId}/qr-token/rotate")
    public ApiResponse<TableResponse> rotateQr(@PathVariable Long branchId, @PathVariable Long tableId) {
        return ApiResponse.success("QR token rotated", restaurantService.rotateTableQrToken(branchId, tableId));
    }
}
