package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.DepartmentDto;
import com.polatholding.procurementsystem.dto.DepartmentFormDto;
import com.polatholding.procurementsystem.model.Department;
import com.polatholding.procurementsystem.repository.DepartmentRepository;
import com.polatholding.procurementsystem.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository,
                                 UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createDepartment(DepartmentFormDto formDto) {
        Department department = new Department();
        BeanUtils.copyProperties(formDto, department, "departmentId");
        departmentRepository.save(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentFormDto getDepartmentFormById(Integer id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        DepartmentFormDto dto = new DepartmentFormDto();
        BeanUtils.copyProperties(dept, dto);
        return dto;
    }

    @Override
    @Transactional
    public void updateDepartment(Integer id, DepartmentFormDto formDto) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
        dept.setDepartmentName(formDto.getDepartmentName());
        dept.setManagerUserId(formDto.getManagerUserId());
        departmentRepository.save(dept);
    }

    @Override
    @Transactional
    public void deleteDepartment(Integer id) {
        if (userRepository.existsByDepartment_DepartmentIdAndFormerEmployeeFalse(id)) {
            throw new IllegalStateException("Department has active employees.");
        }
        departmentRepository.deleteById(id);
    }

    private DepartmentDto convertToDto(Department department) {
        DepartmentDto dto = new DepartmentDto();
        BeanUtils.copyProperties(department, dto);
        return dto;
    }
}
