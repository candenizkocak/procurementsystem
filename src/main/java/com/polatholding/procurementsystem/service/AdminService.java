package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.AdminUserFormDto;
import com.polatholding.procurementsystem.dto.UserDto;
import com.polatholding.procurementsystem.model.Department;
import com.polatholding.procurementsystem.model.Role;

import java.util.List;

public interface AdminService {
    List<UserDto> getAllUsers();
    List<Department> getAllDepartments();
    List<Role> getAllRoles(); // Returns roles suitable for admin assignment
    void createUser(AdminUserFormDto userFormDto);

    // Methods for editing user
    AdminUserFormDto getUserFormById(Integer userId);
    void updateUser(AdminUserFormDto userFormDto);

    // Method for toggling user's active status
    void toggleUserActiveStatus(Integer userId);
}