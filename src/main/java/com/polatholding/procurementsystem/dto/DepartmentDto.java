package com.polatholding.procurementsystem.dto;

import lombok.Data;

@Data
public class DepartmentDto {
    private Integer departmentId;
    private String departmentName;
    private Integer managerUserId;
    private Integer requestCount;
}
