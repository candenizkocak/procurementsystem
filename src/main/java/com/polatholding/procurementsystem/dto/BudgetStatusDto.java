package com.polatholding.procurementsystem.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetStatusDto {
    private String departmentName;
    private String budgetCode;
    private int year;
    private BigDecimal initialAmount;
    private BigDecimal consumedAmount;
    private BigDecimal remainingAmount;
    private double consumptionPercentage;
}