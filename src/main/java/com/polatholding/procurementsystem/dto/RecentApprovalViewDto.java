package com.polatholding.procurementsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecentApprovalViewDto {
    private Integer approvalId;
    private Integer requestId;
    private String approver;
    private String approvalStatus;
    private LocalDateTime approvalDate;
}
