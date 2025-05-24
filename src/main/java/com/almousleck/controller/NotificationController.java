package com.almousleck.controller;

import com.almousleck.dto.auth.ApiResponse;
import com.almousleck.dto.notification.NotificationResponse;
import com.almousleck.security.CurrentUser;
import com.almousleck.security.UserPrincipal;
import com.almousleck.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            Pageable pageable) {
        return ResponseEntity.ok(notificationService.getUserNotifications(currentUser, unreadOnly, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(notificationService.getUnreadCount(currentUser));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(currentUser, notificationId));
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<ApiResponse> markAllAsRead(@CurrentUser UserPrincipal currentUser) {
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok(new ApiResponse(true, "All notifications marked as read"));
    }
}
