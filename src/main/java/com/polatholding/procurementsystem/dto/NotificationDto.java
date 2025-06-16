package com.polatholding.procurementsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Integer notificationId;
    private String message;
    private String link;
    private String sentDateFormatted; // e.g., "5 minutes ago", "Today at 10:30 AM", "Jun 10"
    private boolean isRead;
    private String notificationType;
    private Integer requestId; // To quickly identify the related request if any
}