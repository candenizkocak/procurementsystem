package com.polatholding.procurementsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApprovalViewDto {
    private Integer approvalId;
    private Integer requestId;
    private String approver; // Matches vw_RecentApprovals 'Approver' column (which uses udf_GetUserFullName)
    private String approvalStatus;
    private LocalDateTime approvalDate;
}