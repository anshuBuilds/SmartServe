package com.smartserve.order.service;

import com.smartserve.common.exception.BadRequestException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.menu.entity.MenuItem;
import com.smartserve.menu.repository.MenuItemRepository;
import com.smartserve.order.dto.CreateOrderItemRequest;
import com.smartserve.order.dto.CreateOrderRequest;
import com.smartserve.order.dto.OrderItemResponse;
import com.smartserve.order.dto.OrderResponse;
import com.smartserve.order.dto.UpdateOrderStatusRequest;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.entity.OrderItem;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final MenuItemRepository menuItemRepository;
    private final CustomerOrderRepository customerOrderRepo;

    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }

        CustomerOrder order = new CustomerOrder();
        order.setTableNumber(request.getTableNumber());
        order.setCustomerName(request.getCustomerName().trim());
        order.setSpecialInstructions(request.getSpecialInstructions());
        order.setOrderStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

            if (!menuItem.getAvailable()) {
                throw new BadRequestException("Menu item is not available: " + menuItem.getName());
            }

            BigDecimal unitPrice = menuItem.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setMenuItem(menuItem);
            orderItem.setItemName(menuItem.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setLineTotal(lineTotal);

            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(lineTotal);
        }

        order.setTotalAmount(totalAmount);

        CustomerOrder savedOrder = customerOrderRepo.save(order);
        return toOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return customerOrderRepo.findAll().stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return customerOrderRepo.findByOrderStatus(status).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByTableNumber(Integer tableNumber) {
        return customerOrderRepo.findByTableNumber(tableNumber).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByTableNumberAndStatus(Integer tableNumber, OrderStatus status) {
        return customerOrderRepo.findByTableNumberAndOrderStatus(tableNumber, status).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return toOrderResponse(findOrder(orderId));
    }

    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        CustomerOrder order = findOrder(orderId);
        OrderStatus newStatus = request.getOrderStatus();

        validateStatusTransition(order.getOrderStatus(), newStatus);

        order.setOrderStatus(newStatus);
        return toOrderResponse(order);
    }

    public OrderResponse cancelOrder(Long orderId) {
        CustomerOrder order = findOrder(orderId);

        validateStatusTransition(order.getOrderStatus(), OrderStatus.CANCELLED);

        order.setOrderStatus(OrderStatus.CANCELLED);
        return toOrderResponse(order);
    }

    private CustomerOrder findOrder(Long orderId) {
        return customerOrderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        boolean validTransition = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.PREPARING
                    || newStatus == OrderStatus.CANCELLED;
            case PREPARING -> newStatus == OrderStatus.READY
                    || newStatus == OrderStatus.CANCELLED;
            case READY -> newStatus == OrderStatus.SERVED;
            case SERVED, CANCELLED -> false;
        };

        if (!validTransition) {
            throw new BadRequestException(
                    "Cannot change order status from " + currentStatus + " to " + newStatus
            );
        }
    }

    private OrderResponse toOrderResponse(CustomerOrder order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::toOrderItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getTableNumber(),
                order.getCustomerName(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                order.getSpecialInstructions(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getMenuItem().getId(),
                item.getItemName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
