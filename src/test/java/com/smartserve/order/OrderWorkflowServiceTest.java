package com.smartserve.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.smartserve.common.exception.ConflictException;
import com.smartserve.common.exception.ForbiddenException;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.order.enums.OrderStatus;
import com.smartserve.order.repository.CustomerOrderRepository;
import com.smartserve.order.service.OrderWorkflowService;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.entity.RestaurantTable;
import com.smartserve.restaurant.enums.TableStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderWorkflowServiceTest {
    @Mock CustomerOrderRepository repository;
    private OrderWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new OrderWorkflowService(repository);
    }

    @Test
    void startsPendingTicketAndSetsDedicatedTimestamp() {
        CustomerOrder order = order(OrderStatus.PENDING, 4L);
        when(repository.findById(10L)).thenReturn(Optional.of(order));

        CustomerOrder result = service.startPreparation(10L, 4L);

        assertEquals(OrderStatus.PREPARING, result.getOrderStatus());
        assertNotNull(result.getPreparationStartedAt());
        assertNull(result.getReadyAt());
        verify(repository).flush();
    }

    @Test
    void marksPreparingTicketReady() {
        CustomerOrder order = order(OrderStatus.PREPARING, 4L);
        when(repository.findById(10L)).thenReturn(Optional.of(order));

        CustomerOrder result = service.markReady(10L, 4L);

        assertEquals(OrderStatus.READY, result.getOrderStatus());
        assertNotNull(result.getReadyAt());
    }

    @Test
    void rejectsRepeatedStartWithoutOverwritingTimestamp() {
        CustomerOrder order = order(OrderStatus.PREPARING, 4L);
        when(repository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(ConflictException.class, () -> service.startPreparation(10L, 4L));
        verify(repository, never()).flush();
    }

    @Test
    void rejectsCrossBranchTicketAccess() {
        CustomerOrder order = order(OrderStatus.PENDING, 4L);
        when(repository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(ForbiddenException.class, () -> service.startPreparation(10L, 9L));
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
    }

    @Test
    void servingReadyOrderReleasesTable() {
        CustomerOrder order = order(OrderStatus.READY, 4L);
        order.getTable().setStatus(TableStatus.OCCUPIED);
        when(repository.findById(10L)).thenReturn(Optional.of(order));

        service.markServed(10L);

        assertEquals(OrderStatus.SERVED, order.getOrderStatus());
        assertEquals(TableStatus.AVAILABLE, order.getTable().getStatus());
    }

    private CustomerOrder order(OrderStatus status, Long branchId) {
        Branch branch = new Branch();
        ReflectionTestUtils.setField(branch, "id", branchId);
        RestaurantTable table = new RestaurantTable();
        table.setBranch(branch);
        CustomerOrder order = new CustomerOrder();
        order.setBranch(branch);
        order.setTable(table);
        order.setOrderStatus(status);
        return order;
    }
}
