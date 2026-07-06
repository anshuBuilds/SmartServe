package com.smartserve.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartserve.common.exception.BadRequestException;
import com.smartserve.menu.entity.MenuItem;
import com.smartserve.common.exception.ConflictException;
import com.smartserve.menu.repository.MenuItemRepository;
import com.smartserve.order.dto.CreateOrderItemRequest;
import com.smartserve.order.dto.CreateOrderRequest;
import com.smartserve.order.dto.OrderResponse;
import com.smartserve.order.dto.UpdateOrderStatusRequest;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.enums.OrderType;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.order.service.OrderService;
import com.smartserve.order.service.OrderWorkflowService;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.entity.Restaurant;
import com.smartserve.restaurant.entity.RestaurantTable;
import com.smartserve.restaurant.enums.TableStatus;
import com.smartserve.restaurant.repository.BranchRepository;
import com.smartserve.restaurant.repository.RestaurantTableRepository;
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

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private RestaurantTableRepository tableRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                menuItemRepository,
                customerOrderRepository,
                branchRepository,
                tableRepository,
                new OrderWorkflowService(customerOrderRepository)
        );
    }

    @Test
    void createOrderCalculatesTotalsAndSnapshotsMenuItemData() {
        MenuItem burger = menuItem("Burger", "125.50", true);
        MenuItem fries = menuItem("Fries", "60.00", true);
        stubAvailableTable();
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
        stubAvailableTable();
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
    void createOrderRejectsAnOccupiedTable() {
        Branch branch = branch();
        RestaurantTable table = table(branch);
        table.setStatus(TableStatus.OCCUPIED);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(tableRepository.findForUpdateByIdAndBranchId(4L, 1L))
                .thenReturn(Optional.of(table));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.createOrder(orderRequest(orderItem(1L, 1)))
        );

        assertEquals("Table is not available", exception.getMessage());
        verify(customerOrderRepository, never()).save(any());
    }

    @Test
    void cancellingPendingOrderReleasesItsTable() {
        CustomerOrder order = orderWithStatus(OrderStatus.PENDING);
        order.getTable().setStatus(TableStatus.OCCUPIED);
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.cancelOrder(10L);

        assertEquals(OrderStatus.CANCELLED, response.orderStatus());
        assertEquals(TableStatus.AVAILABLE, order.getTable().getStatus());
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

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> orderService.updateOrderStatus(10L, statusRequest(OrderStatus.SERVED))
        );

        assertEquals("Only READY orders can be served", exception.getMessage());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
    }

    @Test
    void cancelOrderRejectsAnOrderThatIsAlreadyReady() {
        CustomerOrder order = orderWithStatus(OrderStatus.READY);
        when(customerOrderRepository.findById(10L)).thenReturn(Optional.of(order));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> orderService.cancelOrder(10L)
        );

        assertEquals("Only PENDING or PREPARING orders can be cancelled", exception.getMessage());
        assertEquals(OrderStatus.READY, order.getOrderStatus());
    }

    private CreateOrderRequest orderRequest(CreateOrderItemRequest... items) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setBranchId(1L);
        request.setTableId(4L);
        request.setCustomerName("  Table Guest  ");
        request.setOrderType(OrderType.DINE_IN);
        request.setSmsConsent(false);
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
        Branch branch = branch();
        RestaurantTable table = table(branch);
        order.setBranch(branch);
        order.setTable(table);
        order.setCustomerName("Table Guest");
        order.setOrderStatus(status);
        return order;
    }

    private void stubAvailableTable() {
        Branch branch = branch();
        RestaurantTable table = table(branch);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(tableRepository.findForUpdateByIdAndBranchId(4L, 1L))
                .thenReturn(Optional.of(table));
    }

    private Branch branch() {
        Restaurant restaurant = new Restaurant();
        restaurant.setName("SmartServe");
        Branch branch = new Branch();
        branch.setName("Main Branch");
        branch.setRestaurant(restaurant);
        return branch;
    }

    private RestaurantTable table(Branch branch) {
        RestaurantTable table = new RestaurantTable();
        table.setBranch(branch);
        table.setTableNumber("T4");
        table.setCapacity(4);
        table.setStatus(TableStatus.AVAILABLE);
        return table;
    }

    private UpdateOrderStatusRequest statusRequest(OrderStatus status) {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setOrderStatus(status);
        return request;
    }
}

