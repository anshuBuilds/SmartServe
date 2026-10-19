package com.smartserve.payment.controller;

import com.smartserve.common.response.ApiResponse;
import com.smartserve.guest.service.GuestService;
import com.smartserve.order.dto.OrderResponse;
import com.smartserve.payment.dto.VerifyPaymentRequest;
import com.smartserve.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/guest/orders/{trackingToken}/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final GuestService guestService;

    @PostMapping("/verify")
    public ApiResponse<OrderResponse> verifyPayment(
            @PathVariable String trackingToken,
            @Valid @RequestBody VerifyPaymentRequest request
            ) {
        paymentService.verifyGuestPayment(trackingToken, request);

        // Reload inside a read-only transaction: with open-in-view disabled,
        // the entity returned above is detached and its lazy items/branch
        // can no longer be loaded.
        OrderResponse response = guestService.trackedOrder(trackingToken);

        return ApiResponse.success("Payment verified", response);
    }

}
