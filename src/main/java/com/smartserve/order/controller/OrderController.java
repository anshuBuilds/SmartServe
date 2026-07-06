package com.smartserve.order.controller;

import com.smartserve.common.response.ApiResponse;
import com.smartserve.order.dto.CreateOrderRequest;
import com.smartserve.order.dto.OrderResponse;
import com.smartserve.order.dto.UpdateOrderStatusRequest;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created", orderService.createOrder(request)));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long tableId,
            @RequestParam(required = false) OrderStatus status) {
        return ApiResponse.success(resolveOrders(branchId, tableId, status));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrder(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.success("Order status updated", orderService.updateOrderStatus(orderId, request));
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long orderId) {
        return ApiResponse.success("Order cancelled", orderService.cancelOrder(orderId));
    }

    @PatchMapping("/{orderId}/serve")
    public ApiResponse<OrderResponse> serveOrder(@PathVariable Long orderId) {
        return ApiResponse.success("Order served", orderService.serveOrder(orderId));
    }

    private List<OrderResponse> resolveOrders(Long branchId, Long tableId, OrderStatus status) {
        if (tableId != null && status != null) return orderService.getOrdersByTableIdAndStatus(tableId, status);
        if (tableId != null) return orderService.getOrdersByTableId(tableId);
        if (branchId != null && status != null) return orderService.getOrdersByBranchIdAndStatus(branchId, status);
        if (branchId != null) return orderService.getOrdersByBranchId(branchId);
        if (status != null) return orderService.getOrdersByStatus(status);
        return orderService.getAllOrders();
    }
}
