package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    List<Supplier> findByStatus(String status);

    List<Supplier> findByStatusOrderBySupplierNameAsc(String status);
}