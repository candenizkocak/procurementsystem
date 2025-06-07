package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.BudgetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetCodeRepository extends JpaRepository<BudgetCode, Integer> {
}