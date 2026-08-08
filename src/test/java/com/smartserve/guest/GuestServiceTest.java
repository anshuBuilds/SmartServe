package com.smartserve.guest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartserve.common.exception.BadRequestException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.guest.dto.GuestOrderPaymentResponse;
import com.smartserve.guest.dto.GuestOrderRequest;
import com.smartserve.guest.service.GuestService;
import com.smartserve.menu.dto.MenuCategoryResponse;
import com.smartserve.menu.dto.MenuItemResponse;
import com.smartserve.menu.service.MenuService;
import com.smartserve.order.dto.CreateOrderItemRequest;
import com.smartserve.order.dto.CreateOrderRequest;
import com.smartserve.order.dto.OrderResponse;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.enums.OrderType;
import com.smartserve.order.enums.PaymentMethod;
import com.smartserve.order.enums.PaymentStatus;
import com.smartserve.order.service.OrderService;
import com.smartserve.payment.dto.PaymentOrderResponse;
import com.smartserve.payment.service.PaymentService;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.entity.Restaurant;
import com.smartserve.restaurant.entity.RestaurantTable;
import com.smartserve.restaurant.enums.TableStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GuestServiceTest {

    @Mock RestaurantTableRepository tableRepository;
    @Mock MenuService menuService;
    @Mock OrderService orderService;
    @Mock PaymentService paymentService;

    private GuestService service;

    @BeforeEach
    void setUp() {
        service = new GuestService(tableRepository, menuService, orderService, paymentService);
    }

    @Test
    void sessionEnablesOrderingForAvailableTableAtActiveBranch() {
        RestaurantTable table = table(TableStatus.AVAILABLE, true);
        when(tableRepository.findByQrToken("valid-qr")).thenReturn(Optional.of(table));

        var response = service.session("valid-qr");

        assertEquals(2L, response.branchId());
        assertEquals("Main Branch", response.branchName());
        assertEquals("SmartServe", response.restaurantName());
        assertEquals(4L, response.tableId());
        assertEquals("T4", response.tableNumber());
        assertTrue(response.orderingEnabled());
    }

    @Test
    void sessionDisablesOrderingWhenTableIsOccupied() {
        when(tableRepository.findByQrToken("valid-qr"))
                .thenReturn(Optional.of(table(TableStatus.OCCUPIED, true)));

        assertFalse(service.session("valid-qr").orderingEnabled());
    }

    @Test
    void invalidQrTokenIsRejected() {
        when(tableRepository.findByQrToken("expired")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.session("expired")
        );

        assertEquals("QR code is invalid or expired", exception.getMessage());
    }

    @Test
    void menuReturnsOnlyValuesProvidedByMenuService() {
        RestaurantTable table = table(TableStatus.AVAILABLE, true);
        List<MenuCategoryResponse> categories = List.of();
        List<MenuItemResponse> items = List.of();
        when(tableRepository.findByQrToken("valid-qr")).thenReturn(Optional.of(table));
        when(menuService.getActiveCategories()).thenReturn(categories);
        when(menuService.getAvailableItems()).thenReturn(items);

        var response = service.menu("valid-qr");

        assertSame(categories, response.categories());
        assertSame(items, response.items());
    }

    @Test
    void menuRejectsOrderingAtInactiveBranch() {
        when(tableRepository.findByQrToken("valid-qr"))
                .thenReturn(Optional.of(table(TableStatus.AVAILABLE, false)));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.menu("valid-qr")
        );

        assertEquals("Ordering is disabled", exception.getMessage());
        verify(menuService, never()).getActiveCategories();
        verify(menuService, never()).getAvailableItems();
    }

    @Test
    void createOrderRejectsUnavailableTableBeforeCreatingAnything() {
        when(tableRepository.findByQrToken("valid-qr"))
                .thenReturn(Optional.of(table(TableStatus.OCCUPIED, true)));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createOrder("valid-qr", guestRequest())
        );

        assertEquals("Table is not available", exception.getMessage());
        verify(orderService, never()).createGuestOrder(any(), any());
        verify(paymentService, never()).createRazorpayOrder(any());
    }

    @Test
    void createOrderTranslatesGuestRequestAndInitializesPayment() {
        RestaurantTable table = table(TableStatus.AVAILABLE, true);
        GuestOrderRequest guestRequest = guestRequest();
        OrderResponse createdResponse = orderResponse(10L, PaymentStatus.NOT_REQUIRED);
        CustomerOrder savedOrder = new CustomerOrder();
        savedOrder.setTrackingToken("track-123");
        savedOrder.setPaymentStatus(PaymentStatus.NOT_REQUIRED);
        PaymentOrderResponse paymentResponse = new PaymentOrderResponse(
                10L, "track-123", "rzp-order", "key-id", new BigDecimal("250.00"),
                25000, "INR", "Anshu", "******3210"
        );
        OrderResponse updatedResponse = orderResponse(10L, PaymentStatus.PENDING);

        when(tableRepository.findByQrToken("valid-qr")).thenReturn(Optional.of(table));
        when(orderService.createGuestOrder(any(), any())).thenReturn(createdResponse);
        when(orderService.getTrackingToken(10L)).thenReturn("track-123");
        when(orderService.getOrderByTrackingToken("track-123")).thenReturn(savedOrder);
        when(paymentService.createRazorpayOrder(savedOrder)).thenReturn(paymentResponse);
        when(orderService.mapOrder(savedOrder)).thenReturn(updatedResponse);

        GuestOrderPaymentResponse result = service.createOrder("valid-qr", guestRequest);

        ArgumentCaptor<CreateOrderRequest> requestCaptor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        verify(orderService).createGuestOrder(org.mockito.ArgumentMatchers.same(table), requestCaptor.capture());
        CreateOrderRequest translated = requestCaptor.getValue();
        assertEquals("Anshu", translated.getCustomerName());
        assertEquals("9876543210", translated.getCustomerPhone());
        assertTrue(translated.getSmsConsent());
        assertEquals("Less spicy", translated.getSpecialInstructions());
        assertSame(guestRequest.getItems(), translated.getItems());
        assertEquals(OrderType.DINE_IN, translated.getOrderType());
        assertEquals(PaymentMethod.RAZORPAY, savedOrder.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, savedOrder.getPaymentStatus());
        assertEquals("track-123", result.trackingToken());
        assertSame(updatedResponse, result.order());
        assertSame(paymentResponse, result.payment());
    }

    @Test
    void trackedOrderMapsOrderFoundByPrivateTrackingToken() {
        CustomerOrder order = new CustomerOrder();
        OrderResponse response = orderResponse(10L, PaymentStatus.PAID);
        when(orderService.getOrderByTrackingToken("private-token")).thenReturn(order);
        when(orderService.mapOrder(order)).thenReturn(response);

        assertSame(response, service.trackedOrder("private-token"));
    }

    private GuestOrderRequest guestRequest() {
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setMenuItemId(8L);
        item.setQuantity(2);
        GuestOrderRequest request = new GuestOrderRequest();
        request.setCustomerName("Anshu");
        request.setCustomerPhone("9876543210");
        request.setSmsConsent(true);
        request.setSpecialInstructions("Less spicy");
        request.setItems(List.of(item));
        return request;
    }

    private RestaurantTable table(TableStatus status, boolean branchActive) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName("SmartServe");
        Branch branch = new Branch();
        ReflectionTestUtils.setField(branch, "id", 2L);
        branch.setName("Main Branch");
        branch.setActive(branchActive);
        branch.setRestaurant(restaurant);
        RestaurantTable table = new RestaurantTable();
        ReflectionTestUtils.setField(table, "id", 4L);
        table.setBranch(branch);
        table.setTableNumber("T4");
        table.setStatus(status);
        return table;
    }

    private OrderResponse orderResponse(Long id, PaymentStatus paymentStatus) {
        return new OrderResponse(
                id, 2L, "Main Branch", 4L, "T4", "Anshu", OrderType.DINE_IN,
                "******3210", true, OrderStatus.PENDING, paymentStatus, PaymentMethod.RAZORPAY,
                null, new BigDecimal("250.00"), "Less spicy", List.of(), null, null
        );
    }
}
