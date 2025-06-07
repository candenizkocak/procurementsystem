package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Integer> {

    @Query("SELECT pr FROM PurchaseRequest pr " +
            "JOIN FETCH pr.createdByUser u " +
            "LEFT JOIN FETCH u.roles " +
            "JOIN FETCH pr.department " +
            "JOIN FETCH pr.currency " +
            "ORDER BY pr.createdAt DESC")
    List<PurchaseRequest> findAllWithDetails();

    @Query("SELECT pr FROM PurchaseRequest pr " +
            "JOIN FETCH pr.createdByUser u " +
            "LEFT JOIN FETCH u.roles " +
            "JOIN FETCH pr.department " +
            "JOIN FETCH pr.currency " +
            "WHERE u.userId = :userId " +
            "ORDER BY pr.createdAt DESC")
    List<PurchaseRequest> findByCreatorIdWithDetails(@Param("userId") Integer userId);

    /**
     * NEW METHOD: Finds requests waiting at Level 1 for a specific department manager.
     */
    @Query("SELECT pr FROM PurchaseRequest pr " +
            "JOIN FETCH pr.createdByUser " +
            "JOIN FETCH pr.department d " +
            "JOIN FETCH pr.currency " +
            "WHERE pr.status = 'Pending' AND pr.currentApprovalLevel = 1 AND d.managerUserId = :managerId")
    List<PurchaseRequest> findPendingDepartmentManagerApprovals(@Param("managerId") Integer managerId);

    @Query("SELECT pr FROM PurchaseRequest pr " +
            "JOIN ApprovalStep a ON pr.currentApprovalLevel = a.stepOrder " +
            "JOIN FETCH pr.createdByUser " +
            "JOIN FETCH pr.department " +
            "JOIN FETCH pr.currency " +
            "WHERE pr.status = 'Pending' AND a.requiredRole.id IN :roleIds " +
            "ORDER BY pr.createdAt ASC")
    List<PurchaseRequest> findPendingApprovalsByRoleIds(@Param("roleIds") Set<Integer> roleIds);
}