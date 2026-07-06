package com.smartserve.kitchen;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.smartserve.kitchen.service.KitchenService;
import com.smartserve.menu.entity.MenuItem;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.entity.OrderItem;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.order.service.OrderWorkflowService;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.entity.RestaurantTable;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KitchenServiceTest {
    @Mock CustomerOrderRepository repository;
    @Mock OrderWorkflowService workflow;
    private KitchenService service;

    @BeforeEach
    void setUp() {
        service = new KitchenService(repository, workflow);
    }

    @Test
    void queueReturnsCountsAndMaximumPreparationEstimate() {
        CustomerOrder pending = ticket(1L, OrderStatus.PENDING, 8, 15);
        CustomerOrder preparing = ticket(2L, OrderStatus.PREPARING, 5);
        when(repository.findKitchenQueue(eq(4L), anyCollection())).thenReturn(List.of(pending, preparing));

        var response = service.getQueue(4L, null);

        assertEquals(1, response.counts().pending());
        assertEquals(1, response.counts().preparing());
        assertEquals(0, response.counts().ready());
        assertEquals(15, response.tickets().get(0).estimatedPreparationMinutes());
        assertNotNull(response.serverTime());
    }

    @Test
    void queueRejectsTerminalStatusFilter() {
        assertThrows(com.smartserve.common.exception.BadRequestException.class,
                () -> service.getQueue(4L, OrderStatus.SERVED));
        verify(repository, never()).findKitchenQueue(anyLong(), anyCollection());
    }

    private CustomerOrder ticket(Long id, OrderStatus status, int... preparationMinutes) {
        Branch branch = new Branch();
        branch.setName("Main");
        ReflectionTestUtils.setField(branch, "id", 4L);
        RestaurantTable table = new RestaurantTable();
        table.setTableNumber("T4");
        ReflectionTestUtils.setField(table, "id", 9L);
        CustomerOrder order = new CustomerOrder();
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "createdAt", Instant.now());
        order.setBranch(branch);
        order.setTable(table);
        order.setCustomerName("Guest");
        order.setOrderStatus(status);
        for (int index = 0; index < preparationMinutes.length; index++) {
            MenuItem menuItem = new MenuItem();
            menuItem.setPreparationTimeMinutes(preparationMinutes[index]);
            ReflectionTestUtils.setField(menuItem, "id", (long) index + 1);
            OrderItem item = new OrderItem();
            item.setMenuItem(menuItem);
            item.setItemName("Item " + index);
            item.setQuantity(1);
            ReflectionTestUtils.setField(item, "id", (long) index + 10);
            order.getItems().add(item);
        }
        return order;
    }
}
