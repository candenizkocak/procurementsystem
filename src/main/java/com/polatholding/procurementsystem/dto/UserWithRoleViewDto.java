package com.polatholding.procurementsystem.dto;

import lombok.Data;

@Data
public class UserWithRoleViewDto {
    private Integer userId;
    private String firstName;
    private String lastName;
    private String email;
    private String roleName;
    private String departmentName;
}