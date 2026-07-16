package com.smartserve.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.smartserve.common.exception.BadRequestException;
import com.smartserve.common.exception.ConflictException;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.PaymentStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.payment.config.RazorpayProperties;
import com.smartserve.payment.dto.PaymentOrderResponse;
import com.smartserve.payment.dto.VerifyPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final RazorpayProperties razorpayProperties;
    private final CustomerOrderRepository customerOrderRepository;

    public PaymentOrderResponse createRazorpayOrder(CustomerOrder order) {
        if(order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Order amount must be greater than zero");
        }

        if(order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ConflictException("Order has already been paid");
        }

        int amountInPaise = toPaise(order.getTotalAmount());

        try {
            RazorpayClient client = new RazorpayClient(
                    razorpayProperties.keyId(),
                    razorpayProperties.keySecret()
            );

            JSONObject razorpayRequest = new JSONObject();
            razorpayRequest.put("amount", amountInPaise);
            razorpayRequest.put("currency", razorpayProperties.currency());
            razorpayRequest.put("receipt", "smartserve_order_" + order.getId());

            Order razorpayOrder = client.orders.create(razorpayRequest);

            String razorpayOrderId = razorpayOrder.get("id");

            order.setRazorpayOrderId(razorpayOrderId);
            order.setPaymentStatus(PaymentStatus.PENDING);

            CustomerOrder saved = customerOrderRepository.save(order);

            return new PaymentOrderResponse(
                    saved.getId(),
                    saved.getTrackingToken(),
                    razorpayOrderId,
                    razorpayProperties.keyId(),
                    saved.getTotalAmount(),
                    amountInPaise,
                    razorpayProperties.currency(),
                    saved.getCustomerName(),
                    maskPhone(saved.getCustomerPhone())
            );
        } catch (RazorpayException exception) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setPaymentFailureReason(exception.getMessage());
            throw new BadRequestException("Unable to create payment order");
        }
    }

    public CustomerOrder verifyGuestPayment(String trackingToken, VerifyPaymentRequest request) {
        CustomerOrder order  = customerOrderRepository.findByTrackingToken(trackingToken)
                .orElseThrow(() -> new BadRequestException("Order not found"));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ConflictException("Order is already paid");
        }

        if (order.getRazorpayOrderId() == null) {
            throw new BadRequestException("Payment was not initialized for this order");
        }

        if (!order.getRazorpayOrderId().equals(request.getRazorpayOrderId())) {
            throw new BadRequestException("Payment order does not match");
        }

        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getRazorpayOrderId());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
            attributes.put("razorpay_signature", request.getRazorpaySignature());

            Utils.verifyPaymentSignature(attributes, razorpayProperties.keySecret());

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setRazorpayPaymentId(request.getRazorpayPaymentId());
            order.setRazorpaySignature(request.getRazorpaySignature());
            order.setPaymentFailureReason(null);
            order.setPaidAt(Instant.now());

            return customerOrderRepository.save(order);
        } catch (RazorpayException exception) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setPaymentFailureReason("Payment signature verification failed");
            customerOrderRepository.save(order);
            throw new BadRequestException("Payment verification failed");
        }
    }

    private int toPaise(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .intValueExact();
    }

    private String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }

        int visibleDigits = Math.min(4, phone.length());
        return "*".repeat(phone.length() - visibleDigits)
                + phone.substring(phone.length() - visibleDigits);
    }
}
