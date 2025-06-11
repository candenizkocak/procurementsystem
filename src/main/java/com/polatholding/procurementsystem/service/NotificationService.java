package com.polatholding.procurementsystem.service;

public interface NotificationService {
    void sendEmailNotification(String userEmail, Integer requestId, String message);
    void sendAppNotification(String userEmail, Integer requestId, String message);
}
