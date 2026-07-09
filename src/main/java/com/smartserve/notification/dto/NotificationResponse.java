package com.smartserve.notification.dto;

import com.smartserve.notification.enums.NotificationChannel;
import com.smartserve.notification.enums.NotificationStatus;
import com.smartserve.notification.enums.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long branchId,
        String branchName,
        Long orderId,
        Long recipientUserId,
        NotificationChannel channel,
        NotificationType type,
        String title,
        String message,
        NotificationStatus status,
        String errorMessage,
        Instant createdAt,
        Instant sentAt,
        Instant readAt
) {
}
