package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Integer> {

    @Query("SELECT pr FROM PurchaseRequest pr " +
            "JOIN FETCH pr.createdByUser u " +
            "JOIN FETCH pr.department " +
            "JOIN FETCH pr.currency " +
            "ORDER BY pr.createdAt DESC")
    List<PurchaseRequest> findAllWithDetails();

    @Query("SELECT pr FROM PurchaseRequest pr " +
            "JOIN FETCH pr.createdByUser u " +
            "JOIN FETCH pr.department " +
            "JOIN FETCH pr.currency " +
            "WHERE u.userId = :userId " +
            "ORDER BY pr.createdAt DESC")
    List<PurchaseRequest> findByCreatorIdWithDetails(@Param("userId") Integer userId);

    @Query("SELECT pr FROM PurchaseRequest pr " +
            "JOIN FETCH pr.createdByUser " +
            "JOIN FETCH pr.department d " +
            "JOIN FETCH pr.currency " +
            "WHERE pr.status = 'Pending' AND pr.currentApprovalLevel = 1 AND d.managerUserId = :managerId")
    List<PurchaseRequest> findPendingDepartmentManagerApprovals(@Param("managerId") Integer managerId);

    //BELOW IS THE NEW findPendingDepartmentManagerApprovals
    @Query("SELECT pr FROM PurchaseRequest pr " +
            "JOIN FETCH pr.createdByUser " +
            "JOIN FETCH pr.department d " +
            "JOIN FETCH pr.currency " +
            "WHERE pr.status = 'Pending' AND pr.currentApprovalLevel = 1 AND d.departmentId = :departmentId AND d.managerUser.userId = :managerId " +
            "ORDER BY pr.createdAt ASC")
    List<PurchaseRequest> findPendingDepartmentManagerApprovals(@Param("managerId") Integer managerId, @Param("departmentId") Integer departmentId);

    @Query("SELECT pr FROM PurchaseRequest pr " +
            "JOIN ApprovalStep a ON pr.currentApprovalLevel = a.stepOrder " +
            "JOIN FETCH pr.createdByUser " +
            "JOIN FETCH pr.department " +
            "JOIN FETCH pr.currency " +
            "WHERE pr.status = 'Pending' AND a.requiredRole.id IN :roleIds " +
            "ORDER BY pr.createdAt ASC")
    List<PurchaseRequest> findPendingApprovalsByRoleIds(@Param("roleIds") Set<Integer> roleIds);

    @Query("SELECT pr FROM PurchaseRequest pr " +
            "JOIN FETCH pr.createdByUser " +
            "JOIN FETCH pr.department " +
            "JOIN FETCH pr.currency " +
            "JOIN FETCH pr.budgetCode " +
            "LEFT JOIN FETCH pr.items i " +
            "LEFT JOIN FETCH i.supplier " +
            "LEFT JOIN FETCH i.unit " +
            "WHERE pr.requestId = :requestId")
    Optional<PurchaseRequest> findByIdWithAllDetails(@Param("requestId") Integer requestId);

    @Query("SELECT COALESCE(SUM(pr.netAmount), 0) FROM PurchaseRequest pr WHERE pr.budgetCode.id = :budgetCodeId AND pr.status = 'Approved'")
    BigDecimal getConsumedAmountForBudget(@Param("budgetCodeId") Integer budgetCodeId);

    @Query("SELECT pr FROM PurchaseRequest pr " +
            "JOIN FETCH pr.currency " +
            "WHERE pr.budgetCode.id = :budgetCodeId AND pr.status = 'Approved'")
    List<PurchaseRequest> findApprovedByBudget(@Param("budgetCodeId") Integer budgetCodeId);

    @Query("SELECT pr FROM PurchaseRequest pr " +
            "JOIN FETCH pr.createdByUser " +
            "JOIN FETCH pr.department " +
            "JOIN FETCH pr.currency " +
            "WHERE pr.budgetCode.id = :budgetCodeId " +
            "ORDER BY pr.createdAt DESC")
    List<PurchaseRequest> findByBudget(@Param("budgetCodeId") Integer budgetCodeId);

    @Query(value = "SELECT pr.* FROM PurchaseRequests pr " +
            "JOIN PurchaseRequestItems pri ON pr.RequestID = pri.RequestID " +
            "WHERE FREETEXT((pri.ItemName, pri.Description), :searchTerm)",
            nativeQuery = true)
    List<PurchaseRequest> searchByItemFreetext(@Param("searchTerm") String searchTerm);


}