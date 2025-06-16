package com.polatholding.procurementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID")
    private Integer notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) // Assuming RequestID can be null for system notifications
    @JoinColumn(name = "RequestID", nullable = true) // Changed from false based on schema
    private PurchaseRequest purchaseRequest;


    @Column(name = "NotificationType", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "SentDate", nullable = false)
    private LocalDateTime sentDate;

    @Column(name = "IsSuccess", nullable = false) // This might refer to the successful sending/creation of the notification
    private boolean isSuccess = true;

    // New fields
    @Column(name = "Message", length = 500)
    private String message;

    @Column(name = "Link", length = 255)
    private String link;

    @Column(name = "IsRead", nullable = false)
    private boolean isRead = false;

    @PrePersist
    protected void onCreate() {
        sentDate = LocalDateTime.now();
    }
}