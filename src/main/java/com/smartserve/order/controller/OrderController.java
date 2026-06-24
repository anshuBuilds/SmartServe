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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created", order));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders(
            @RequestParam(required = false) Integer tableNumber,
            @RequestParam(required = false) OrderStatus status
    ) {
        List<OrderResponse> orders = resolveOrders(tableNumber, status);
        return ApiResponse.success(orders);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrder(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse order = orderService.updateOrderStatus(orderId, request);
        return ApiResponse.success("Order status updated", order);
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long orderId) {
        OrderResponse order = orderService.cancelOrder(orderId);
        return ApiResponse.success("Order cancelled", order);
    }

    private List<OrderResponse> resolveOrders(Integer tableNumber, OrderStatus status) {
        if (tableNumber != null && status != null) {
            return orderService.getOrdersByTableNumberAndStatus(tableNumber, status);
        }

        if (tableNumber != null) {
            return orderService.getOrdersByTableNumber(tableNumber);
        }

        if (status != null) {
            return orderService.getOrdersByStatus(status);
        }

        return orderService.getAllOrders();
    }
}
