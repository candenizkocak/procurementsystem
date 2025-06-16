package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.NotificationDto;
import com.polatholding.procurementsystem.model.*;
import com.polatholding.procurementsystem.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApprovalStepRepository approvalStepRepository; // To get role for next approver


    // Notification Types Constants
    public static final String TYPE_APPROVAL_REQUIRED = "APPROVAL_REQUIRED";
    public static final String TYPE_REQUEST_UPDATE = "REQUEST_UPDATE";
    public static final String TYPE_REQUEST_APPROVED = "REQUEST_APPROVED";
    public static final String TYPE_REQUEST_REJECTED = "REQUEST_REJECTED";
    public static final String TYPE_REQUEST_RETURNED = "REQUEST_RETURNED";


    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   ApprovalStepRepository approvalStepRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.approvalStepRepository = approvalStepRepository;
    }

    @Override
    @Transactional
    public void createNotification(User targetUser, PurchaseRequest request, String notificationType, String message, String link) {
        if (targetUser == null) {
            log.warn("Target user is null for notification. Type: {}, RequestID: {}", notificationType, request != null ? request.getRequestId() : "N/A");
            return;
        }
        Notification notification = new Notification();
        notification.setUser(targetUser);
        notification.setPurchaseRequest(request); // Can be null for system-wide notifications if schema allowed
        notification.setNotificationType(notificationType);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setSentDate(LocalDateTime.now());
        notification.setRead(false);
        notification.setSuccess(true); // Assuming creation means success of sending intent

        notificationRepository.save(notification);
        log.info("Notification created for UserID: {}, Type: {}, RequestID: {}", targetUser.getUserId(), notificationType, request != null ? request.getRequestId() : "N/A");
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getRecentNotificationsForUser(Integer userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("sentDate").descending());
        List<Notification> notifications = notificationRepository.findByUserUserId(userId, pageable);
        return notifications.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadNotificationCountForUser(Integer userId) {
        return notificationRepository.countByUserUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markNotificationAsRead(Integer notificationId, Integer userId) {
        // Direct update using native SQL query to ensure IsRead is set to 1 in the database
        notificationRepository.markAsReadNative(notificationId, userId);
        log.info("NotificationID: {} marked as read for UserID: {} using native query", notificationId, userId);
    }

    @Override
    @Transactional
    public void markAllNotificationsAsRead(Integer userId) {
        notificationRepository.markAllAsReadForUser(userId);
        log.info("All unread notifications marked as read for UserID: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getAllNotificationsForUser(Integer userId) {
        // Get all notifications for the user, sorted by sent date in descending order
        List<Notification> notifications = notificationRepository.findByUserUserIdOrderBySentDateDesc(userId);
        return notifications.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setNotificationId(notification.getNotificationId());
        dto.setMessage(notification.getMessage());
        dto.setLink(notification.getLink());
        dto.setRead(notification.isRead());
        dto.setNotificationType(notification.getNotificationType());
        dto.setSentDateFormatted(formatSentDate(notification.getSentDate()));
        if (notification.getPurchaseRequest() != null) {
            dto.setRequestId(notification.getPurchaseRequest().getRequestId());
        }
        return dto;
    }

    private String formatSentDate(LocalDateTime sentDate) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(sentDate, now);

        if (duration.toMinutes() < 1) {
            return "Just now";
        } else if (duration.toHours() < 1) {
            return duration.toMinutes() + "m ago";
        } else if (duration.toDays() < 1) {
            return duration.toHours() + "h ago";
        } else if (duration.toDays() < 7) {
            return duration.toDays() + "d ago";
        } else if (now.getYear() == sentDate.getYear()) {
            return sentDate.format(DateTimeFormatter.ofPattern("MMM d"));
        } else {
            return sentDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        }
    }

    // --- Specific Notification Creation Logic ---

    @Override
    @Transactional
    public void notifyRequestSubmission(PurchaseRequest request) {
        String message;
        String link = "/requests/" + request.getRequestId();
        List<User> targetApprovers = getNextApprovers(request);

        for (User approver : targetApprovers) {
            // Avoid notifying self if user is also the first approver (e.g., a Manager submitting for themselves going to Proc.Man)
            if (approver.getUserId().equals(request.getCreatedByUser().getUserId()) && request.getCurrentApprovalLevel() == 1) { // Only skip if it's the very first level self-approval
                boolean isOnlyApproverAtLevel1 = request.getDepartment().getManagerUser() != null && request.getDepartment().getManagerUser().getUserId().equals(approver.getUserId());
                if(isOnlyApproverAtLevel1 && targetApprovers.size() == 1) { // If they are the dept manager and no other manager, it must go to level 2
                    log.info("Skipping self-notification for UserID {} on RequestID {} as they are the first approver (Dept. Manager). Request goes to next level.", approver.getUserId(), request.getRequestId());
                    continue;
                }
            }
            message = "Request #" + request.getRequestId() + " from " + request.getCreatedByUser().getFirstName() + " is awaiting your approval.";
            createNotification(approver, request, TYPE_APPROVAL_REQUIRED, message, link);
        }
    }


    @Override
    @Transactional
    public void notifyApprovalStep(PurchaseRequest request, User approver, String previousStatus) {
        // Notify creator
        String creatorMessage = "Your request #" + request.getRequestId() + " was " + request.getStatus().toLowerCase() +
                " by " + approver.getFirstName() + ".";
        String link = "/requests/" + request.getRequestId();

        List<User> nextApprovers = getNextApprovers(request);
        if (!nextApprovers.isEmpty()) {
            String nextApproverRoleName = nextApprovers.get(0).getRoles().iterator().next().getRoleName(); // Simplified
            creatorMessage += " Now pending " + nextApproverRoleName + " approval.";
        }
        createNotification(request.getCreatedByUser(), request, TYPE_REQUEST_UPDATE, creatorMessage, link);

        // Notify next approvers
        if (!"Approved".equalsIgnoreCase(request.getStatus())) { // Only notify next if not fully approved yet
            String approverMessage = "Request #" + request.getRequestId() + " (approved by " + approver.getFirstName() + ") requires your approval.";
            for (User nextApprover : nextApprovers) {
                if (nextApprover.getUserId().equals(request.getCreatedByUser().getUserId())) {
                    log.info("Skipping self-notification for next step for UserID {} on RequestID {}.", nextApprover.getUserId(), request.getRequestId());
                    continue;
                }
                createNotification(nextApprover, request, TYPE_APPROVAL_REQUIRED, approverMessage, link);
            }
        }
    }

    @Override
    @Transactional
    public void notifyFinalApproval(PurchaseRequest request, User finalApprover) {
        String message = "Your request #" + request.getRequestId() + " has been fully approved by " + finalApprover.getFirstName() + ".";
        String link = "/requests/" + request.getRequestId();
        createNotification(request.getCreatedByUser(), request, TYPE_REQUEST_APPROVED, message, link);
    }

    @Override
    @Transactional
    public void notifyRejection(PurchaseRequest request, User approver) {
        String message = "Your request #" + request.getRequestId() + " was rejected by " + approver.getFirstName() + ". Reason: " + request.getRejectReason();
        String link = "/requests/" + request.getRequestId();
        createNotification(request.getCreatedByUser(), request, TYPE_REQUEST_REJECTED, message, link);
    }

    @Override
    @Transactional
    public void notifyReturnForEdit(PurchaseRequest request, User approver) {
        String message = "Request #" + request.getRequestId() + " was returned for edit by " + approver.getFirstName() + ". Comments: " + request.getRejectReason();
        String link = "/requests/" + request.getRequestId() + "/edit";
        createNotification(request.getCreatedByUser(), request, TYPE_REQUEST_RETURNED, message, link);
    }

    private List<User> getNextApprovers(PurchaseRequest request) {
        List<User> approvers = new ArrayList<>();
        if ("Pending".equalsIgnoreCase(request.getStatus())) {
            int nextLevel = request.getCurrentApprovalLevel();
            ApprovalStep approvalStep = approvalStepRepository.findByStepOrder(nextLevel).orElse(null);

            if (approvalStep != null) {
                Role requiredRole = approvalStep.getRequiredRole();
                if (AdminServiceImpl.MANAGER_ROLE_NAME.equals(requiredRole.getRoleName()) && request.getDepartment().getManagerUser() != null) {
                    approvers.add(request.getDepartment().getManagerUser());
                } else {
                    // For ProcurementManager, Director, etc.
                    List<User> usersInRole = userRepository.findByRolesContaining(requiredRole);
                    approvers.addAll(usersInRole);
                }
            } else {
                log.warn("No approval step found for order {} for requestID {}", nextLevel, request.getRequestId());
            }
        }
        return approvers.stream().distinct().collect(Collectors.toList()); // Ensure distinct users
    }
}

