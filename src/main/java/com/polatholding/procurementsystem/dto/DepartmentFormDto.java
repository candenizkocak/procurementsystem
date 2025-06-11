package com.polatholding.procurementsystem.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class DepartmentFormDto {
    private Integer departmentId;

    @NotEmpty(message = "Department name cannot be empty.")
    private String departmentName;

    private Integer managerUserId;
}
