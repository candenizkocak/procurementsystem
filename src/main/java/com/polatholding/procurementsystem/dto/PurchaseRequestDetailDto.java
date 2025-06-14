package com.polatholding.procurementsystem.dto;

import com.polatholding.procurementsystem.model.PurchaseRequestItem;
import com.polatholding.procurementsystem.model.File;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseRequestDetailDto {
    private Integer requestId;
    private String creatorFullName;
    private String departmentName;
    private String status;
    private String budgetCode;
    private String rejectReason;
    private LocalDateTime createdAt;
    private BigDecimal netAmount;
    private BigDecimal grossAmount;
    private String currencyCode;
    private Integer daysSinceCreated;
    private Integer itemCount;
    private java.time.LocalDateTime lastApprovalDate;
    private boolean highValue;
    private List<PurchaseRequestItem> items; // We can pass the entity here for simplicity
    private List<File> files;
}