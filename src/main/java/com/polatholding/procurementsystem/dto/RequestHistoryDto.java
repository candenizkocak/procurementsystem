package com.polatholding.procurementsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RequestHistoryDto {
    private Integer historyId;
    private Integer requestId;
    private String userEmail;
    private String action;
    private String details;
    private LocalDateTime eventDate;
}
