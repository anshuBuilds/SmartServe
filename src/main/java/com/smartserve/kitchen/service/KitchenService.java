package com.smartserve.kitchen.service;

import com.smartserve.common.exception.BadRequestException;
import com.smartserve.common.exception.ForbiddenException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.kitchen.dto.KitchenHistoryResponse;
import com.smartserve.kitchen.dto.KitchenQueueCounts;
import com.smartserve.kitchen.dto.KitchenQueueResponse;
import com.smartserve.kitchen.dto.KitchenTicketItemResponse;
import com.smartserve.kitchen.dto.KitchenTicketResponse;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.entity.OrderItem;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.order.service.OrderWorkflowService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KitchenService {
    private static final List<OrderStatus> ACTIVE = List.of(
            OrderStatus.PENDING, OrderStatus.PREPARING, OrderStatus.READY);
    private static final List<OrderStatus> HISTORY = List.of(
            OrderStatus.READY, OrderStatus.SERVED, OrderStatus.CANCELLED);

    private final CustomerOrderRepository orderRepository;
    private final OrderWorkflowService workflowService;

    @Value("${app.business-zone:Asia/Kolkata}")
    private String businessZone;

    public KitchenQueueResponse getQueue(Long branchId, OrderStatus status) {
        List<OrderStatus> statuses = status == null ? ACTIVE : List.of(requireActiveStatus(status));
        List<KitchenTicketResponse> tickets = orderRepository.findKitchenQueue(branchId, statuses)
                .stream().map(this::toTicket).toList();
        return new KitchenQueueResponse(Instant.now(), new KitchenQueueCounts(
                count(tickets, OrderStatus.PENDING),
                count(tickets, OrderStatus.PREPARING),
                count(tickets, OrderStatus.READY)), tickets);
    }

    public KitchenTicketResponse getTicket(Long branchId, Long orderId) {
        CustomerOrder order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        requireBranch(order, branchId);
        return toTicket(order);
    }

    @Transactional
    public KitchenTicketResponse start(Long branchId, Long orderId) {
        return toTicket(workflowService.startPreparation(orderId, branchId));
    }

    @Transactional
    public KitchenTicketResponse ready(Long branchId, Long orderId) {
        return toTicket(workflowService.markReady(orderId, branchId));
    }

    public KitchenHistoryResponse getHistory(
            Long branchId, Instant from, Instant to, OrderStatus status, int page, int size) {
        if (page < 0) throw new BadRequestException("page must be zero or greater");
        if (size < 1 || size > 100) throw new BadRequestException("size must be between 1 and 100");

        ZoneId zone = ZoneId.of(businessZone);
        LocalDate today = LocalDate.now(zone);
        Instant effectiveFrom = from == null ? today.atStartOfDay(zone).toInstant() : from;
        Instant effectiveTo = to == null ? today.plusDays(1).atStartOfDay(zone).toInstant() : to;
        if (!effectiveFrom.isBefore(effectiveTo)) throw new BadRequestException("from must be before to");
        if (Duration.between(effectiveFrom, effectiveTo).compareTo(Duration.ofDays(31)) > 0) {
            throw new BadRequestException("History range cannot exceed 31 days");
        }

        List<OrderStatus> statuses = status == null ? HISTORY : List.of(requireHistoryStatus(status));
        Page<Long> ids = orderRepository.findKitchenHistoryIds(
                branchId, statuses, effectiveFrom, effectiveTo, PageRequest.of(page, size));
        List<CustomerOrder> orders = ids.isEmpty()
                ? List.of() : orderRepository.findDetailedByIdIn(ids.getContent());
        orders = new ArrayList<>(orders);
        orders.sort(Comparator.comparing(CustomerOrder::getCreatedAt).reversed());
        return new KitchenHistoryResponse(Instant.now(), page, size, ids.getTotalElements(),
                ids.getTotalPages(), orders.stream().map(this::toTicket).toList());
    }

    private OrderStatus requireActiveStatus(OrderStatus status) {
        if (!ACTIVE.contains(status)) throw new BadRequestException("Kitchen queue status must be PENDING, PREPARING, or READY");
        return status;
    }

    private OrderStatus requireHistoryStatus(OrderStatus status) {
        if (!HISTORY.contains(status)) throw new BadRequestException("History status must be READY, SERVED, or CANCELLED");
        return status;
    }

    private void requireBranch(CustomerOrder order, Long branchId) {
        if (!order.getBranch().getId().equals(branchId)) {
            throw new ForbiddenException("Ticket does not belong to your branch");
        }
    }

    private long count(List<KitchenTicketResponse> tickets, OrderStatus status) {
        return tickets.stream().filter(ticket -> ticket.status() == status).count();
    }

    private KitchenTicketResponse toTicket(CustomerOrder order) {
        List<KitchenTicketItemResponse> items = order.getItems().stream().map(this::toItem).toList();
        int estimate = items.stream().map(KitchenTicketItemResponse::preparationTimeMinutes)
                .filter(value -> value != null).max(Integer::compareTo).orElse(0);
        return new KitchenTicketResponse(
                order.getId(), order.getBranch().getId(), order.getBranch().getName(),
                order.getTable() == null ? null : order.getTable().getId(),
                order.getTable() == null ? null : order.getTable().getTableNumber(),
                order.getCustomerName(),
                order.getOrderStatus(), order.getSpecialInstructions(), order.getCreatedAt(),
                order.getPreparationStartedAt(), order.getReadyAt(), estimate, items);
    }

    private KitchenTicketItemResponse toItem(OrderItem item) {
        return new KitchenTicketItemResponse(item.getId(), item.getMenuItem().getId(), item.getItemName(),
                item.getQuantity(), item.getMenuItem().getPreparationTimeMinutes());
    }
}

