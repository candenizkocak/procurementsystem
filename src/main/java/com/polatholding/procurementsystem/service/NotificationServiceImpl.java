package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.model.Notification;
import com.polatholding.procurementsystem.model.User;
import com.polatholding.procurementsystem.repository.NotificationRepository;
import com.polatholding.procurementsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void sendEmailNotification(String userEmail, Integer requestId, String message) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        boolean success = true;
        try {
            // Placeholder for real email sending logic
            System.out.printf("Email to %s: %s%n", userEmail, message);
        } catch (Exception e) {
            success = false;
        }
        if (user != null) {
            logNotification(user, requestId, "EMAIL", success);
        }
    }

    @Override
    @Transactional
    public void sendAppNotification(String userEmail, Integer requestId, String message) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        boolean success = true;
        try {
            // Placeholder for real app notification logic
            System.out.printf("App notification to %s: %s%n", userEmail, message);
        } catch (Exception e) {
            success = false;
        }
        if (user != null) {
            logNotification(user, requestId, "APP", success);
        }
    }

    private void logNotification(User user, Integer requestId, String type, boolean success) {
        Notification log = new Notification();
        log.setUser(user);
        log.setRequestId(requestId);
        log.setNotificationType(type);
        log.setSentDate(LocalDateTime.now());
        log.setSuccess(success);
        notificationRepository.save(log);
    }
}
