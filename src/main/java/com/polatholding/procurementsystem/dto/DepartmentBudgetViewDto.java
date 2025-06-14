package com.polatholding.procurementsystem.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DepartmentBudgetViewDto {
    private String departmentName;
    private String code;
    private String description;
    private BigDecimal budgetAmount;
    private int year;
}