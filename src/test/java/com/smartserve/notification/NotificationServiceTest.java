package com.smartserve.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartserve.common.exception.ForbiddenException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.notification.entity.Notification;
import com.smartserve.notification.enums.NotificationChannel;
import com.smartserve.notification.enums.NotificationStatus;
import com.smartserve.notification.enums.NotificationType;
import com.smartserve.notification.repository.NotificationRepository;
import com.smartserve.notification.service.NotificationService;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.entity.RestaurantTable;
import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import com.smartserve.user.repository.UserRepository;
import java.time.Instant;
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
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, userRepository);
    }

    @Test
    void orderCreatedCreatesOneInAppNotificationPerBranchUser() {
        CustomerOrder order = orderWithTable("T4");
        UserEntity chef = user(11L);
        UserEntity waiter = user(12L);
        when(userRepository.findByBranchId(2L)).thenReturn(List.of(chef, waiter));

        service.notifyOrderCreated(order);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<Notification> saved = captor.getAllValues();
        assertEquals(List.of(chef, waiter), saved.stream().map(Notification::getRecipientUser).toList());
        assertTrue(saved.stream().allMatch(n -> n.getType() == NotificationType.ORDER_CREATED));
        assertTrue(saved.stream().allMatch(n -> n.getChannel() == NotificationChannel.IN_APP));
        assertTrue(saved.stream().allMatch(n -> n.getStatus() == NotificationStatus.PENDING));
        assertTrue(saved.stream().allMatch(n -> n.getMessage().equals("New order received for Table T4.")));
        assertTrue(saved.stream().allMatch(n -> n.getOrder() == order));
    }

    @Test
    void eventMethodsUseTheirOwnTypeTitleAndMessage() {
        CustomerOrder order = orderWithTable("T7");
        when(userRepository.findByBranchId(2L)).thenReturn(List.of(user(11L)));

        service.notifyOrderReady(order);
        service.notifyOrderCancelled(order);
        service.notifyOrderServed(order);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        List<Notification> notifications = captor.getAllValues();
        assertEquals(NotificationType.ORDER_READY, notifications.get(0).getType());
        assertEquals("Order ready", notifications.get(0).getTitle());
        assertEquals("Table T7 order is ready to serve.", notifications.get(0).getMessage());
        assertEquals(NotificationType.ORDER_CANCELLED, notifications.get(1).getType());
        assertEquals("Order cancelled", notifications.get(1).getTitle());
        assertEquals(NotificationType.ORDER_SERVED, notifications.get(2).getType());
        assertEquals("Order served", notifications.get(2).getTitle());
    }

    @Test
    void takeawayOrderUsesTakeawayLabel() {
        CustomerOrder order = orderWithTable(null);
        when(userRepository.findByBranchId(2L)).thenReturn(List.of(user(11L)));

        service.notifyOrderReady(order);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("Takeaway order is ready to serve.", captor.getValue().getMessage());
    }

    @Test
    void noBranchUsersCreatesNoNotifications() {
        CustomerOrder order = orderWithTable("T4");
        when(userRepository.findByBranchId(2L)).thenReturn(List.of());

        service.notifyOrderCreated(order);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void getMyNotificationsMapsRepositoryEntitiesToResponses() {
        Notification notification = notification(20L, user(11L), NotificationStatus.PENDING);
        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(11L))
                .thenReturn(List.of(notification));

        var responses = service.getMyNotifications(11L);

        assertEquals(1, responses.size());
        var response = responses.get(0);
        assertEquals(20L, response.id());
        assertEquals(2L, response.branchId());
        assertEquals("Main Branch", response.branchName());
        assertEquals(10L, response.orderId());
        assertEquals(11L, response.recipientUserId());
        assertEquals(NotificationStatus.PENDING, response.status());
    }

    @Test
    void unreadQueriesAlwaysUsePendingStatus() {
        when(notificationRepository.findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
                11L, NotificationStatus.PENDING)).thenReturn(List.of());
        when(notificationRepository.countByRecipientUserIdAndStatus(11L, NotificationStatus.PENDING))
                .thenReturn(3L);

        assertTrue(service.getMyUnreadNotifications(11L).isEmpty());
        assertEquals(3L, service.getMyUnreadCount(11L));
    }

    @Test
    void markAsReadOnlyChangesNotificationOwnedByCurrentUser() {
        UserEntity owner = user(11L);
        Notification notification = notification(20L, owner, NotificationStatus.PENDING);
        when(notificationRepository.findById(20L)).thenReturn(Optional.of(notification));

        var response = service.markAsRead(20L, 11L);

        assertEquals(NotificationStatus.READ, notification.getStatus());
        assertNotNull(notification.getReadAt());
        assertEquals(NotificationStatus.READ, response.status());
        assertEquals(notification.getReadAt(), response.readAt());
    }

    @Test
    void markAsReadRejectsAnotherUsersNotification() {
        Notification notification = notification(20L, user(12L), NotificationStatus.PENDING);
        when(notificationRepository.findById(20L)).thenReturn(Optional.of(notification));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.markAsRead(20L, 11L)
        );

        assertEquals("You cannot access this notification", exception.getMessage());
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
    }

    @Test
    void markAsReadReturnsNotFoundForMissingNotification() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.markAsRead(99L, 11L)
        );

        assertEquals("Notification not found", exception.getMessage());
    }

    @Test
    void markAllAsReadUpdatesOnlyPendingNotificationsForUser() {
        Notification first = notification(20L, user(11L), NotificationStatus.PENDING);
        Notification second = notification(21L, user(11L), NotificationStatus.PENDING);
        when(notificationRepository.findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
                11L, NotificationStatus.PENDING)).thenReturn(List.of(first, second));

        service.markAllAsRead(11L);

        assertEquals(NotificationStatus.READ, first.getStatus());
        assertEquals(NotificationStatus.READ, second.getStatus());
        assertNotNull(first.getReadAt());
        assertSame(first.getReadAt(), second.getReadAt());
    }

    private CustomerOrder orderWithTable(String tableNumber) {
        Branch branch = branch();
        CustomerOrder order = new CustomerOrder();
        ReflectionTestUtils.setField(order, "id", 10L);
        order.setBranch(branch);
        order.setCustomerPhone("9876543210");
        if (tableNumber != null) {
            RestaurantTable table = new RestaurantTable();
            table.setTableNumber(tableNumber);
            table.setBranch(branch);
            order.setTable(table);
        }
        return order;
    }

    private Notification notification(Long id, UserEntity recipient, NotificationStatus status) {
        Notification notification = new Notification();
        ReflectionTestUtils.setField(notification, "id", id);
        ReflectionTestUtils.setField(notification, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        notification.setBranch(branch());
        notification.setOrder(orderWithTable("T4"));
        notification.setRecipientUser(recipient);
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setType(NotificationType.ORDER_CREATED);
        notification.setTitle("New order created");
        notification.setMessage("New order received for Table T4.");
        notification.setStatus(status);
        return notification;
    }

    private Branch branch() {
        Branch branch = new Branch();
        ReflectionTestUtils.setField(branch, "id", 2L);
        branch.setName("Main Branch");
        return branch;
    }

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        ReflectionTestUtils.setField(user, "id", id);
        user.setRole(Role.WAITER);
        return user;
    }
}
