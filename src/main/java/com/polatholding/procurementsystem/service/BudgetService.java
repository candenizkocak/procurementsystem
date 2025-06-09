package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.BudgetDto;
import com.polatholding.procurementsystem.dto.BudgetFormDto;
import com.polatholding.procurementsystem.model.Department;

import java.util.List;

public interface BudgetService {
    List<BudgetDto> getAllBudgets();
    List<Department> getAllDepartments();
    void createNewBudget(BudgetFormDto formDto);
    BudgetFormDto getBudgetFormById(Integer budgetId);
    void updateBudget(Integer budgetId, BudgetFormDto formDto);
}