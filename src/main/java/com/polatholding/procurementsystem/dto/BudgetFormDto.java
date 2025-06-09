package com.polatholding.procurementsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BudgetFormDto {
    private Integer budgetCodeId;

    @NotNull(message = "A department must be selected.")
    private Integer departmentId;

    @NotEmpty(message = "Budget code cannot be empty.")
    @Size(max = 30, message = "Code cannot exceed 30 characters.")
    private String code;

    @Size(max = 200, message = "Description cannot exceed 200 characters.")
    private String description;

    @NotNull(message = "Year cannot be empty.")
    @Min(value = 2020, message = "Year must be 2020 or later.")
    private Integer year;

    @NotNull(message = "Budget amount cannot be empty.")
    @Min(value = 0, message = "Budget amount cannot be negative.")
    private BigDecimal budgetAmount;

    private Boolean isActive = true;
}