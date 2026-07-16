package com.smartserve.guest.controller;

import com.smartserve.common.response.ApiResponse;
import com.smartserve.guest.dto.*;
import com.smartserve.guest.service.GuestService;
import com.smartserve.order.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/guest") @RequiredArgsConstructor
public class GuestController {
    private final GuestService guestService;
    @GetMapping("/session/{token}")
    public ApiResponse<GuestSessionResponse> session(@PathVariable String token) {
        return ApiResponse.success(guestService.session(token));
    }
    @GetMapping("/menu/{token}")
    public ApiResponse<GuestMenuResponse> menu(@PathVariable String token) {
        return ApiResponse.success(guestService.menu(token));
    }
    @PostMapping("/orders/{token}")
    public ResponseEntity<ApiResponse<GuestOrderPaymentResponse>> order(
            @PathVariable String token, @Valid @RequestBody GuestOrderRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
        .body(ApiResponse.success("Order created", guestService.createOrder(token, request)));
    }
    @GetMapping("/orders/{trackingToken}")
    public ApiResponse<OrderResponse> tracked(@PathVariable String trackingToken) {
        return ApiResponse.success(guestService.trackedOrder(trackingToken));
    }
}
