package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.BudgetDto;
import com.polatholding.procurementsystem.dto.BudgetFormDto;
import com.polatholding.procurementsystem.model.BudgetCode;
import com.polatholding.procurementsystem.model.Department;
import com.polatholding.procurementsystem.repository.BudgetCodeRepository;
import com.polatholding.procurementsystem.repository.DepartmentRepository;
import com.polatholding.procurementsystem.repository.UserRepository;
import com.polatholding.procurementsystem.service.NotificationService;
import com.polatholding.procurementsystem.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetCodeRepository budgetCodeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public BudgetServiceImpl(BudgetCodeRepository budgetCodeRepository,
                             DepartmentRepository departmentRepository,
                             UserRepository userRepository,
                             NotificationService notificationService) {
        this.budgetCodeRepository = budgetCodeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetDto> getAllBudgets() {
        return budgetCodeRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Override
    @Transactional
    public void createNewBudget(BudgetFormDto formDto) {
        BudgetCode budgetCode = new BudgetCode();
        Department department = departmentRepository.findById(formDto.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        // For creation, BeanUtils is safe because the target ID is null.
        BeanUtils.copyProperties(formDto, budgetCode, "budgetCodeId");
        budgetCode.setDepartment(department);
        budgetCodeRepository.save(budgetCode);
        User user = getCurrentUser();
        if (user != null) {
            notificationService.sendAppNotification(user.getEmail(), 0, "Budget created: " + budgetCode.getCode());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetFormDto getBudgetFormById(Integer budgetId) {
        BudgetCode budgetCode = budgetCodeRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        BudgetFormDto formDto = new BudgetFormDto();
        BeanUtils.copyProperties(budgetCode, formDto);
        if (budgetCode.getDepartment() != null) {
            formDto.setDepartmentId(budgetCode.getDepartment().getDepartmentId());
        }
        return formDto;
    }

    @Override
    @Transactional
    public void updateBudget(Integer budgetId, BudgetFormDto formDto) {
        // 1. Fetch the existing entity from the database.
        BudgetCode budgetCodeToUpdate = budgetCodeRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + budgetId));

        // 2. Fetch the associated department.
        Department department = departmentRepository.findById(formDto.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + formDto.getDepartmentId()));

        // 3. **THE FIX**: Manually map fields from the DTO to the entity.
        // DO NOT use BeanUtils.copyProperties for updates, as it can corrupt the ID.
        budgetCodeToUpdate.setDepartment(department);
        budgetCodeToUpdate.setCode(formDto.getCode());
        budgetCodeToUpdate.setDescription(formDto.getDescription());
        budgetCodeToUpdate.setYear(formDto.getYear());
        budgetCodeToUpdate.setBudgetAmount(formDto.getBudgetAmount());
        budgetCodeToUpdate.setActive(formDto.getIsActive());

        // 4. Save the updated entity. Hibernate will generate an UPDATE statement.
        budgetCodeRepository.save(budgetCodeToUpdate);
        User user = getCurrentUser();
        if (user != null) {
            notificationService.sendAppNotification(user.getEmail(), 0, "Budget updated: " + budgetCodeToUpdate.getCode());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetDto getBudgetById(Integer budgetId) {
        BudgetCode budgetCode = budgetCodeRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + budgetId));
        return convertToDto(budgetCode);
    }

    private BudgetDto convertToDto(BudgetCode budgetCode) {
        BudgetDto dto = new BudgetDto();
        BeanUtils.copyProperties(budgetCode, dto);
        if (budgetCode.getDepartment() != null) {
            dto.setDepartmentName(budgetCode.getDepartment().getDepartmentName());
        }
        return dto;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
}