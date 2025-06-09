package com.polatholding.procurementsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserDto {
    private Integer userId;
    private String firstName;
    private String lastName;
    private String email;
    private String departmentName;
    private Set<String> roles; // Store role names as Strings
    private LocalDateTime createdAt;
    private boolean formerEmployee;
}