package com.smartserve.notification.service;

import com.smartserve.common.exception.ForbiddenException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.notification.dto.NotificationResponse;
import com.smartserve.notification.entity.Notification;
import com.smartserve.notification.enums.NotificationChannel;
import com.smartserve.notification.enums.NotificationStatus;
import com.smartserve.notification.enums.NotificationType;
import com.smartserve.notification.repository.NotificationRepository;
import com.smartserve.order.entity.CustomerOrder;
import com.smartserve.restaurant.entity.RestaurantTable;
import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public void notifyOrderCreated(CustomerOrder order) {
        createBranchNotifications(
                order,
                NotificationType.ORDER_CREATED,
                "New order created",
                "New order received for " + tableLabel(order) + "."
        );
    }

    public void notifyOrderReady(CustomerOrder order) {
        createBranchNotifications(
                order,
                NotificationType.ORDER_READY,
                "Order ready",
                tableLabel(order) + " order is ready to serve."
        );
    }

    public void notifyOrderCancelled(CustomerOrder order) {
        createBranchNotifications(
                order,
                NotificationType.ORDER_CANCELLED,
                "Order cancelled",
                tableLabel(order) + " order has been cancelled."
        );
    }

    public void notifyOrderServed(CustomerOrder order) {
        createBranchNotifications(
                order,
                NotificationType.ORDER_SERVED,
                "Order served",
                tableLabel(order) + " order has been served."
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long userId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyUnreadNotifications(Long userId) {
        return notificationRepository
                .findByRecipientUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getMyUnreadCount(Long userId) {
        return notificationRepository.countByRecipientUserIdAndStatus(userId, NotificationStatus.PENDING);
    }

    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = findUserNotification(notificationId, userId);
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(Instant.now());
        return toResponse(notification);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository
                .findByRecipientUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.PENDING);
        Instant now = Instant.now();
        notifications.forEach(notification -> {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(now);
        });
    }

    private Notification findUserNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (notification.getRecipientUser() == null
                || !notification.getRecipientUser().getId().equals(userId)) {
            throw new ForbiddenException("You cannot access this notification");
        }

        return notification;
    }

    private void createBranchNotifications(
            CustomerOrder order,
            NotificationType type,
            String title,
            String message
    ) {
        List<UserEntity> recipients = userRepository.findByBranchId(order.getBranch().getId());

        for (UserEntity recipient : recipients) {
            Notification notification = new Notification();
            notification.setBranch(order.getBranch());
            notification.setOrder(order);
            notification.setRecipientUser(recipient);
            notification.setRecipientPhone(order.getCustomerPhone());
            notification.setChannel(NotificationChannel.IN_APP);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(notification);
        }
    }

    private String tableLabel(CustomerOrder order) {
        RestaurantTable table = order.getTable();
        if (table != null) {
            return "Table " + table.getTableNumber();
        }
        return "Takeaway";
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getBranch() == null ? null : notification.getBranch().getId(),
                notification.getBranch() == null ? null : notification.getBranch().getName(),
                notification.getOrder() == null ? null : notification.getOrder().getId(),
                notification.getRecipientUser() == null ? null : notification.getRecipientUser().getId(),
                notification.getChannel(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getErrorMessage(),
                notification.getCreatedAt(),
                notification.getSentAt(),
                notification.getReadAt()
        );
    }
}
