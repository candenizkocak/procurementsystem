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

    @Column(name = "RequestID", nullable = false)
    private Integer requestId;

    @Column(name = "NotificationType", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "SentDate", nullable = false)
    private LocalDateTime sentDate;

    @Column(name = "IsSuccess", nullable = false)
    private boolean isSuccess;
}