package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.DepartmentDto;
import com.polatholding.procurementsystem.dto.DepartmentFormDto;

import java.util.List;

public interface DepartmentService {
    List<DepartmentDto> getAllDepartments();
    void createDepartment(DepartmentFormDto formDto);
    DepartmentFormDto getDepartmentFormById(Integer id);
    void updateDepartment(Integer id, DepartmentFormDto formDto);
    void deleteDepartment(Integer id);
}
