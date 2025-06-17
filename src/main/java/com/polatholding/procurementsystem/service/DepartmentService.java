package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.DepartmentDto;
import com.polatholding.procurementsystem.dto.DepartmentFormDto;

import java.util.List;

public interface DepartmentService {
    List<DepartmentDto> getAllDepartments();
    void createDepartment(DepartmentFormDto formDto);
    DepartmentFormDto getDepartmentFormById(Integer id);
    void updateDepartment(Integer id, DepartmentFormDto formDto);

    /**
     * Attempts to delete a department.
     * @param id The ID of the department to delete
     * @return true if deletion was successful, false if department has active employees
     */
    boolean deleteDepartment(Integer id);

    /**
     * Checks if a department has any active employees.
     * @param id The ID of the department to check
     * @return true if the department has active employees, false otherwise
     */
    boolean hasDepartmentActiveEmployees(Integer id);
}
