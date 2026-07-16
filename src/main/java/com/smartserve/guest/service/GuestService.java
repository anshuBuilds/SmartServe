package com.smartserve.guest.service;

import com.smartserve.common.exception.BadRequestException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.guest.dto.*;
import com.smartserve.menu.service.MenuService;
import com.smartserve.order.dto.CreateOrderRequest;
import com.smartserve.order.dto.OrderResponse;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.OrderType;
import com.smartserve.order.enums.PaymentMethod;
import com.smartserve.order.enums.PaymentStatus;
import com.smartserve.order.service.OrderService;
import com.smartserve.payment.dto.PaymentOrderResponse;
import com.smartserve.payment.service.PaymentService;
import com.smartserve.restaurant.entity.RestaurantTable;
import com.smartserve.restaurant.enums.TableStatus;
import com.smartserve.restaurant.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class GuestService {
    private final RestaurantTableRepository tableRepository;
    private final MenuService menuService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    @Transactional(readOnly = true)
    public GuestSessionResponse session(String token) {
        RestaurantTable table = findTable(token);
        return new GuestSessionResponse(table.getBranch().getId(), table.getBranch().getName(),
                table.getBranch().getRestaurant().getName(), table.getId(), table.getTableNumber(),
                table.getStatus(), table.getStatus() == TableStatus.AVAILABLE && table.getBranch().getActive());
    }

    @Transactional(readOnly = true)
    public GuestMenuResponse menu(String token) {
        RestaurantTable table = findTable(token);
        if (!Boolean.TRUE.equals(table.getBranch().getActive())) throw new BadRequestException("Ordering is disabled");
        return new GuestMenuResponse(menuService.getActiveCategories(), menuService.getAvailableItems());
    }

    @Transactional
    public GuestOrderPaymentResponse createOrder(String token, GuestOrderRequest guest) {
        RestaurantTable table = findTable(token);
        if (table.getStatus() != TableStatus.AVAILABLE) throw new BadRequestException("Table is not available");
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName(guest.getCustomerName());
        request.setCustomerPhone(guest.getCustomerPhone());
        request.setSmsConsent(guest.getSmsConsent());
        request.setSpecialInstructions(guest.getSpecialInstructions());
        request.setItems(guest.getItems());
        request.setOrderType(OrderType.DINE_IN);

        OrderResponse response = orderService.createGuestOrder(table, request);

        CustomerOrder savedOrder = orderService.getOrderByTrackingToken(
                orderService.getOrderByTrackingToken(findTrackingToken(response.id())).getTrackingToken());

        savedOrder.setPaymentMethod(PaymentMethod.RAZORPAY);
        savedOrder.setPaymentStatus(PaymentStatus.PENDING);

        PaymentOrderResponse payment = paymentService.createRazorpayOrder(savedOrder);
        OrderResponse updatedOrder = orderService.mapOrder(savedOrder);

        return new GuestOrderPaymentResponse(
                savedOrder.getTrackingToken(),
                updatedOrder,
                payment
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse trackedOrder(String trackingToken) {
        return orderService.mapOrder(orderService.getOrderByTrackingToken(trackingToken));
    }

    private RestaurantTable findTable(String token) {
        return tableRepository.findByQrToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("QR code is invalid or expired"));
    }

    private String findTrackingToken(Long orderId) {
        // The response intentionally omits private tracking credentials; fetch the just-created entity.
        return orderService.getTrackingToken(orderId);
    }
}
