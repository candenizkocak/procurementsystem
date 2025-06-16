package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.NotificationDto;
import com.polatholding.procurementsystem.model.PurchaseRequest;
import com.polatholding.procurementsystem.model.User;

import java.util.List;

public interface NotificationService {

    void createNotification(User targetUser, PurchaseRequest request, String notificationType, String message, String link);

    List<NotificationDto> getRecentNotificationsForUser(Integer userId, int limit);

    List<NotificationDto> getAllNotificationsForUser(Integer userId);

    long getUnreadNotificationCountForUser(Integer userId);

    void markNotificationAsRead(Integer notificationId, Integer userId);

    void markAllNotificationsAsRead(Integer userId);

    // Helper methods for generating notifications (can be private in impl or exposed if needed elsewhere)
    void notifyRequestSubmission(PurchaseRequest request);
    void notifyApprovalStep(PurchaseRequest request, User approver, String previousStatus);
    void notifyFinalApproval(PurchaseRequest request, User finalApprover);
    void notifyRejection(PurchaseRequest request, User approver);
    void notifyReturnForEdit(PurchaseRequest request, User approver);
}