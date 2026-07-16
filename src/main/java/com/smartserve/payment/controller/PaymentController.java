package com.smartserve.payment.controller;

import com.smartserve.common.response.ApiResponse;
import com.smartserve.order.dto.OrderResponse;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.service.OrderService;
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
    private final OrderService orderService;

    @PostMapping("/verify")
    public ApiResponse<OrderResponse> verifyPayment(
            @PathVariable String trackingToken,
            @Valid @RequestBody VerifyPaymentRequest request
            ) {
        CustomerOrder paidOrder = paymentService.verifyGuestPayment(trackingToken, request);
        OrderResponse response = orderService.mapOrder(paidOrder);

        return ApiResponse.success("Payment verified", response);
    }

}
