package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.config.security.CustomUserDetails;
import com.polatholding.procurementsystem.dto.NotificationDto;
import com.polatholding.procurementsystem.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadNotificationCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        long count = notificationService.getUnreadNotificationCountForUser(userDetails.getUserId());
        return ResponseEntity.ok(count);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<NotificationDto>> getRecentNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "5") int limit) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        List<NotificationDto> notifications = notificationService.getRecentNotificationsForUser(userDetails.getUserId(), limit);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/all")
    public ResponseEntity<List<NotificationDto>> getAllNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        List<NotificationDto> notifications = notificationService.getAllNotificationsForUser(userDetails.getUserId());
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer notificationId) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        notificationService.markNotificationAsRead(notificationId, userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        notificationService.markAllNotificationsAsRead(userDetails.getUserId());
        return ResponseEntity.ok().build();
    }
}