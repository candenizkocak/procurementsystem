package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.RequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Integer> {
}