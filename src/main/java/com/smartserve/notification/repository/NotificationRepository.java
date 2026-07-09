package com.smartserve.notification.repository;

import com.smartserve.notification.entity.Notification;
import com.smartserve.notification.enums.NotificationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    List<Notification> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    List<Notification> findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
            Long recipientUserId,
            NotificationStatus status
    );

    long countByRecipientUserIdAndStatus(Long recipientUserId, NotificationStatus status);
}
