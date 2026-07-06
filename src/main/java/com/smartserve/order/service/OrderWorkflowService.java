package com.smartserve.order.service;

import com.smartserve.common.exception.ConflictException;
import com.smartserve.common.exception.ForbiddenException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.restaurant.enums.TableStatus;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderWorkflowService {
    private final CustomerOrderRepository orderRepository;

    public CustomerOrder startPreparation(Long orderId, Long branchId) {
        CustomerOrder order = findForBranch(orderId, branchId);
        requireStatus(order, OrderStatus.PENDING, "Only PENDING tickets can be started");
        order.setOrderStatus(OrderStatus.PREPARING);
        order.setPreparationStartedAt(Instant.now());
        orderRepository.flush();
        return order;
    }

    public CustomerOrder markReady(Long orderId, Long branchId) {
        CustomerOrder order = findForBranch(orderId, branchId);
        requireStatus(order, OrderStatus.PREPARING, "Only PREPARING tickets can be marked ready");
        order.setOrderStatus(OrderStatus.READY);
        order.setReadyAt(Instant.now());
        orderRepository.flush();
        return order;
    }

    public CustomerOrder markServed(Long orderId) {
        CustomerOrder order = find(orderId);
        requireStatus(order, OrderStatus.READY, "Only READY orders can be served");
        order.setOrderStatus(OrderStatus.SERVED);
        order.getTable().setStatus(TableStatus.AVAILABLE);
        orderRepository.flush();
        return order;
    }

    public CustomerOrder cancel(Long orderId) {
        CustomerOrder order = find(orderId);
        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.PREPARING) {
            throw new ConflictException("Only PENDING or PREPARING orders can be cancelled");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.getTable().setStatus(TableStatus.AVAILABLE);
        orderRepository.flush();
        return order;
    }

    public CustomerOrder transitionAsManager(Long orderId, OrderStatus target) {
        CustomerOrder order = find(orderId);
        switch (target) {
            case PREPARING -> {
                requireStatus(order, OrderStatus.PENDING, "Only PENDING tickets can be started");
                order.setOrderStatus(OrderStatus.PREPARING);
                order.setPreparationStartedAt(Instant.now());
            }
            case READY -> {
                requireStatus(order, OrderStatus.PREPARING, "Only PREPARING tickets can be marked ready");
                order.setOrderStatus(OrderStatus.READY);
                order.setReadyAt(Instant.now());
            }
            case SERVED -> {
                requireStatus(order, OrderStatus.READY, "Only READY orders can be served");
                order.setOrderStatus(OrderStatus.SERVED);
                order.getTable().setStatus(TableStatus.AVAILABLE);
            }
            case CANCELLED -> {
                if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.PREPARING) {
                    throw new ConflictException("Only PENDING or PREPARING orders can be cancelled");
                }
                order.setOrderStatus(OrderStatus.CANCELLED);
                order.getTable().setStatus(TableStatus.AVAILABLE);
            }
            case PENDING -> throw new ConflictException("Orders cannot transition back to PENDING");
        }
        orderRepository.flush();
        return order;
    }

    @Transactional(readOnly = true)
    public CustomerOrder find(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private CustomerOrder findForBranch(Long orderId, Long branchId) {
        CustomerOrder order = find(orderId);
        if (!order.getBranch().getId().equals(branchId)) {
            throw new ForbiddenException("Ticket does not belong to your branch");
        }
        return order;
    }

    private void requireStatus(CustomerOrder order, OrderStatus expected, String message) {
        if (order.getOrderStatus() != expected) {
            throw new ConflictException(message);
        }
    }
}

