package com.smartserve.kitchen.controller;

import com.smartserve.common.response.ApiResponse;
import com.smartserve.common.security.BranchAccessService;
import com.smartserve.kitchen.dto.KitchenHistoryResponse;
import com.smartserve.kitchen.dto.KitchenQueueResponse;
import com.smartserve.kitchen.dto.KitchenTicketResponse;
import com.smartserve.kitchen.service.KitchenService;
import com.smartserve.order.enums.OrderStatus;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kitchen/tickets")
@RequiredArgsConstructor
public class KitchenController {
    private final KitchenService kitchenService;
    private final BranchAccessService branchAccessService;

    @GetMapping
    public ApiResponse<KitchenQueueResponse> queue(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) OrderStatus status,
            Authentication authentication) {
        return ApiResponse.success(kitchenService.getQueue(
                branchAccessService.resolveKitchenBranch(authentication, branchId), status));
    }

    @GetMapping("/history")
    public ApiResponse<KitchenHistoryResponse> history(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long authorizedBranch = branchAccessService.resolveKitchenBranch(authentication, branchId);
        return ApiResponse.success(kitchenService.getHistory(authorizedBranch, from, to, status, page, size));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<KitchenTicketResponse> ticket(
            @PathVariable Long orderId,
            @RequestParam(required = false) Long branchId,
            Authentication authentication) {
        return ApiResponse.success(kitchenService.getTicket(
                branchAccessService.resolveKitchenBranch(authentication, branchId), orderId));
    }

    @PatchMapping("/{orderId}/start")
    public ApiResponse<KitchenTicketResponse> start(
            @PathVariable Long orderId,
            @RequestParam(required = false) Long branchId,
            Authentication authentication) {
        return ApiResponse.success("Preparation started", kitchenService.start(
                branchAccessService.resolveKitchenBranch(authentication, branchId), orderId));
    }

    @PatchMapping("/{orderId}/ready")
    public ApiResponse<KitchenTicketResponse> ready(
            @PathVariable Long orderId,
            @RequestParam(required = false) Long branchId,
            Authentication authentication) {
        return ApiResponse.success("Ticket marked ready", kitchenService.ready(
                branchAccessService.resolveKitchenBranch(authentication, branchId), orderId));
    }
}
