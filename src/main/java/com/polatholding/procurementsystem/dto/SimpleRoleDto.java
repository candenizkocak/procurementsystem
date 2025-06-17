package com.polatholding.procurementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleRoleDto {
    private Integer roleId;
    private String roleName;
}