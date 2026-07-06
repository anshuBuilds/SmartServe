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
import com.smartserve.order.enums.OrderType;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.entity.RestaurantTable;
import com.smartserve.restaurant.enums.TableStatus;
import com.smartserve.restaurant.repository.BranchRepository;
import com.smartserve.restaurant.repository.RestaurantTableRepository;
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
    private final BranchRepository branchRepository;
    private final RestaurantTableRepository tableRepository;
    private final OrderWorkflowService workflowService;

    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Order must contain at least one item");
        }
        if (request.getOrderType() == null) {
            throw new BadRequestException("Order type is required");
        }
        if (request.getSmsConsent() == null) {
            throw new BadRequestException("SMS consent choice is required");
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        RestaurantTable table = null;

        if (request.getOrderType() == OrderType.DINE_IN) {
            if (request.getTableId() == null) {
                throw new BadRequestException("Table ID is required for dine-in orders");
            }

            table = tableRepository
                    .findForUpdateByIdAndBranchId(
                            request.getTableId(),
                            request.getBranchId()
                    )
                    .orElseThrow(() -> new ResourceNotFoundException("Table not found in this branch"));

            if (table.getStatus() != TableStatus.AVAILABLE) {
                throw new BadRequestException("Table is not available");
            }
        } else if (request.getTableId() != null) {
            throw new BadRequestException("Takeaway orders must not contain a table ID");
        }

        String customerPhone = normalizePhone(request.getCustomerPhone());
        if (request.getOrderType() == OrderType.TAKEAWAY && customerPhone == null) {
            throw new BadRequestException("Customer phone is required for takeaway orders");
        }
        if (Boolean.TRUE.equals(request.getSmsConsent()) && customerPhone == null) {
            throw new BadRequestException("Customer phone is required when SMS consent is enabled");
        }

        CustomerOrder order = new CustomerOrder();
        order.setBranch(branch);
        order.setTable(table);
        order.setCustomerName(request.getCustomerName().trim());
        order.setOrderType(request.getOrderType());
        order.setCustomerPhone(customerPhone);
        order.setSmsConsent(request.getSmsConsent());
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
        if (table != null) {
            table.setStatus(TableStatus.OCCUPIED);
        }

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
    public List<OrderResponse> getOrdersByTableId(Long tableId) {
        return customerOrderRepo.findByTableId(tableId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByTableIdAndStatus(Long tableId, OrderStatus status) {
        return customerOrderRepo.findByTableIdAndOrderStatus(tableId, status).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByBranchId(Long branchId) {
        return customerOrderRepo.findByBranchId(branchId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByBranchIdAndStatus(Long branchId, OrderStatus status) {
        return customerOrderRepo.findByBranchIdAndOrderStatus(branchId, status).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return toOrderResponse(findOrder(orderId));
    }

    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        return toOrderResponse(workflowService.transitionAsManager(orderId, request.getOrderStatus()));
    }

    public OrderResponse cancelOrder(Long orderId) {
        return toOrderResponse(workflowService.cancel(orderId));
    }

    private CustomerOrder findOrder(Long orderId) {
        return customerOrderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    public OrderResponse serveOrder(Long orderId) {
        return toOrderResponse(workflowService.markServed(orderId));
    }

    private OrderResponse toOrderResponse(CustomerOrder order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::toOrderItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getBranch().getId(),
                order.getBranch().getName(),
                order.getTable() == null ? null : order.getTable().getId(),
                order.getTable() == null ? null : order.getTable().getTableNumber(),
                order.getCustomerName(),
                order.getOrderType(),
                maskPhone(order.getCustomerPhone()),
                order.getSmsConsent(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                order.getSpecialInstructions(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        String normalized = phone.replaceAll("[\\s()-]", "");
        if (!normalized.matches("^\\+[1-9]\\d{7,14}$")) {
            throw new BadRequestException("Customer phone must use international format");
        }
        return normalized;
    }
    private String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        int visibleDigits = Math.min(4, phone.length());
        return "*".repeat(phone.length() - visibleDigits)
                + phone.substring(phone.length() - visibleDigits);
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



