package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, Integer> {
}