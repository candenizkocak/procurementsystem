package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    List<Supplier> findByStatus(String status);

    List<Supplier> findByStatusOrderBySupplierNameAsc(String status);

    @Query(value = "SELECT s.* FROM Suppliers s " +
            "WHERE FREETEXT((s.SupplierName, s.ContactPerson, s.Address), :searchTerm) " +
            "AND s.Status = 'Active'",
            nativeQuery = true)
    List<Supplier> searchByFreetext(@Param("searchTerm") String searchTerm);
}