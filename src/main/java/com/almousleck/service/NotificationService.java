package com.almousleck.service;

import com.almousleck.dto.notification.NotificationResponse;
import com.almousleck.exception.ResourceNotFoundException;
import com.almousleck.model.Notification;
import com.almousleck.model.User;
import com.almousleck.repository.NotificationRepository;
import com.almousleck.repository.UserRepository;
import com.almousleck.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public Page<NotificationResponse> getUserNotifications(UserPrincipal currentUser, boolean unreadOnly, Pageable pageable) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        Page<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false, pageable);
        } else {
            notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        }

        return notifications.map(this::convertToNotificationResponse);
    }

    public long getUnreadCount(UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        return notificationRepository.countByUserAndIsRead(user, false);
    }

    @Transactional
    public NotificationResponse markAsRead(UserPrincipal currentUser, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to access this notification");
        }

        notification.setRead(true);
        Notification updatedNotification = notificationRepository.save(notification);

        return convertToNotificationResponse(updatedNotification);
    }

    @Transactional
    public void markAllAsRead(UserPrincipal currentUser) {
        notificationRepository.markAllAsRead(currentUser.getId());
    }

    @Transactional
    public void createNotification(User user, String title, String message, Notification.NotificationType type) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    private NotificationResponse convertToNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType().name())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

