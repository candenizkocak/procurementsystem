package com.polatholding.procurementsystem.dto;

import com.polatholding.procurementsystem.validation.OnCreate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserFormDto {

    private Integer userId;

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

    @NotEmpty(groups = OnCreate.class, message = "Password is required for new users.")
    @Size(groups = OnCreate.class, min = 8, message = "Password must be at least 8 characters long for new users.")
    private String password;

    private Integer departmentId;

    @NotNull(message = "A role must be selected.")
    private Integer roleId;

    private boolean formerEmployee = false;
}