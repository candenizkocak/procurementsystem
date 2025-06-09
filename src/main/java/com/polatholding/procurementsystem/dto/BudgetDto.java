package com.polatholding.procurementsystem.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetDto {
    private Integer budgetCodeId;
    private String code;
    private String description;
    private String departmentName; // Important for display
    private int year;
    private BigDecimal budgetAmount;
    private boolean isActive;
}