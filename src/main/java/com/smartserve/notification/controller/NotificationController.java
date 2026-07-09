package com.smartserve.notification.controller;

import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.common.response.ApiResponse;
import com.smartserve.notification.dto.NotificationResponse;
import com.smartserve.notification.service.NotificationService;
import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.repository.UserRepository;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getMyNotifications(Principal principal) {
        return ApiResponse.success(notificationService.getMyNotifications(currentUser(principal).getId()));
    }

    @GetMapping("/unread")
    public ApiResponse<List<NotificationResponse>> getMyUnreadNotifications(Principal principal) {
        return ApiResponse.success(notificationService.getMyUnreadNotifications(currentUser(principal).getId()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getMyUnreadCount(Principal principal) {
        return ApiResponse.success(notificationService.getMyUnreadCount(currentUser(principal).getId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @PathVariable Long notificationId,
            Principal principal
    ) {
        return ApiResponse.success(
                "Notification marked as read",
                notificationService.markAsRead(notificationId, currentUser(principal).getId())
        );
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(Principal principal) {
        notificationService.markAllAsRead(currentUser(principal).getId());
        return ApiResponse.success("Notifications marked as read", null);
    }

    private UserEntity currentUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
