package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.BudgetCode;
import com.polatholding.procurementsystem.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetCodeRepository extends JpaRepository<BudgetCode, Integer> {

    List<BudgetCode> findByDepartmentAndIsActiveTrue(Department department); // This was the missing method
}