package com.polatholding.procurementsystem.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PurchaseRequestDto {
    private Integer requestId;
    private String creatorFullName;
    private String departmentName;
    private String status;
    private BigDecimal netAmount;
    private String currencyCode;
    private LocalDateTime createdAt;
    private String rejectReason;
    private Integer creatorId;
}