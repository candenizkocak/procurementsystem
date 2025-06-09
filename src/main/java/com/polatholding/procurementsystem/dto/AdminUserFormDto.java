package com.polatholding.procurementsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserFormDto {

    private Integer userId; // For edit mode

    @NotEmpty(message = "First name cannot be empty.")
    @Size(max = 50)
    private String firstName;

    @NotEmpty(message = "Last name cannot be empty.")
    @Size(max = 50)
    private String lastName;

    @NotEmpty(message = "Email cannot be empty.")
    @Email(message = "Invalid email format.")
    @Size(max = 100)
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters long.")
    // For create: required. For edit: optional (if blank, password not changed)
    private String password;

    // DepartmentId can be null if Auditor, Director, or Admin role is selected
    private Integer departmentId;

    @NotNull(message = "A role must be selected.")
    private Integer roleId; // Single role ID

    private boolean formerEmployee = false;
}