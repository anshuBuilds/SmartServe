package com.smartserve.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartserve.common.exception.BadRequestException;
import com.smartserve.menu.entity.MenuItem;
import com.smartserve.menu.repository.MenuItemRepository;
import com.smartserve.order.dto.CreateOrderItemRequest;
import com.smartserve.order.dto.CreateOrderRequest;
import com.smartserve.order.dto.OrderResponse;
import com.smartserve.order.dto.UpdateOrderStatusRequest;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.order.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(menuItemRepository, customerOrderRepository);
    }

    @Test
    void createOrderCalculatesTotalsAndSnapshotsMenuItemData() {
        MenuItem burger = menuItem("Burger", "125.50", true);
        MenuItem fries = menuItem("Fries", "60.00", true);
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(burger));
        when(menuItemRepository.findById(2L)).thenReturn(Optional.of(fries));
        when(customerOrderRepository.save(any(CustomerOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderRequest request = orderRequest(
                orderItem(1L, 2),
                orderItem(2L, 1)
        );

        OrderResponse response = orderService.createOrder(request);

        assertEquals(new BigDecimal("311.00"), response.totalAmount());
        assertEquals(OrderStatus.PENDING, response.orderStatus());
        assertEquals(2, response.items().size());
        assertEquals("Burger", response.items().get(0).itemName());
        assertEquals(new BigDecimal("251.00"), response.items().get(0).lineTotal());

        ArgumentCaptor<CustomerOrder> savedOrder = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(customerOrderRepository).save(savedOrder.capture());
        assertEquals("Table Guest", savedOrder.getValue().getCustomerName());
        assertEquals(savedOrder.getValue(), savedOrder.getValue().getItems().get(0).getOrder());
    }

    @Test
    void createOrderRejectsAnEmptyItemList() {
        CreateOrderRequest request = orderRequest();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals("Order must contain at least one item", exception.getMessage());
        verify(customerOrderRepository, never()).save(any());
    }

    @Test
    void createOrderRejectsUnavailableMenuItem() {
        when(menuItemRepository.findById(1L))
                .thenReturn(Optional.of(menuItem("Sold-out Burger", "125.50", false)));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.createOrder(orderRequest(orderItem(1L, 1)))
        );

        assertEquals("Menu item is not available: Sold-out Burger", exception.getMessage());
        verify(customerOrderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatusAllowsTheNextWorkflowStep() {
        CustomerOrder order = orderWithStatus(OrderStatus.PENDING);
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        UpdateOrderStatusRequest request = statusRequest(OrderStatus.PREPARING);

        OrderResponse response = orderService.updateOrderStatus(10L, request);

        assertEquals(OrderStatus.PREPARING, response.orderStatus());
        assertEquals(OrderStatus.PREPARING, order.getOrderStatus());
    }

    @Test
    void updateOrderStatusRejectsSkippingWorkflowSteps() {
        CustomerOrder order = orderWithStatus(OrderStatus.PENDING);
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(order));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.updateOrderStatus(10L, statusRequest(OrderStatus.SERVED))
        );

        assertEquals("Cannot change order status from PENDING to SERVED", exception.getMessage());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
    }

    @Test
    void cancelOrderRejectsAnOrderThatIsAlreadyReady() {
        CustomerOrder order = orderWithStatus(OrderStatus.READY);
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(order));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.cancelOrder(10L)
        );

        assertEquals("Cannot change order status from READY to CANCELLED", exception.getMessage());
        assertEquals(OrderStatus.READY, order.getOrderStatus());
    }

    private CreateOrderRequest orderRequest(CreateOrderItemRequest... items) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setTableNumber(4);
        request.setCustomerName("  Table Guest  ");
        request.setSpecialInstructions("No onions");
        request.setItems(List.of(items));
        return request;
    }

    private CreateOrderItemRequest orderItem(Long menuItemId, int quantity) {
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setMenuItemId(menuItemId);
        item.setQuantity(quantity);
        return item;
    }

    private MenuItem menuItem(String name, String price, boolean available) {
        MenuItem item = new MenuItem();
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        item.setAvailable(available);
        return item;
    }

    private CustomerOrder orderWithStatus(OrderStatus status) {
        CustomerOrder order = new CustomerOrder();
        order.setTableNumber(4);
        order.setCustomerName("Table Guest");
        order.setOrderStatus(status);
        return order;
    }

    private UpdateOrderStatusRequest statusRequest(OrderStatus status) {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setOrderStatus(status);
        return request;
    }
}
