package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Integer> {
    // Find the next step in the workflow
    Optional<ApprovalStep> findByStepOrder(int stepOrder);
}