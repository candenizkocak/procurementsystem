package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.dto.RequestSummaryViewDto;

import java.math.BigDecimal;
import java.util.List;

public interface DatabaseHelperRepository {
    void addUser(String firstName, String lastName, String email, String passwordHash, Integer departmentId);
    void assignUserRole(Integer userId, String roleName);
    void addSupplier(String supplierName, String contactPerson, String email);
    void updateExchangeRate(String currencyCode, BigDecimal rate);
    void logHistoryAction(Integer requestId, Integer userId, String action, String details);
    List<RequestSummaryViewDto> searchRequests(String searchTerm);
    List<RequestSummaryViewDto> getPendingApprovalsForManager(Integer managerId);
    RequestSummaryViewDto getRequestSummary(Integer requestId);
    List<RequestSummaryViewDto> getRequestItemsDetail(Integer requestId);
    int getDaysSinceRequest(Integer requestId);
    BigDecimal calculateGrossAmount(BigDecimal netAmount);
    void startBackupJob();

    // Additional UDF utilities
    String getRequestStatus(Integer requestId);
    String getUserFullNameById(Integer userId);
    int getDepartmentRequestCount(Integer departmentId);
    boolean isHighValueTRY(Integer requestId);
    String getApproverRoleForLevel(Integer level);
    BigDecimal totalRequestValueForUser(Integer userId);
    java.time.LocalDateTime getLastApprovalDate(Integer requestId);
    int getRequestItemCount(Integer requestId);

    // Stored procedure utilities
    void updateRequestStatus(Integer requestId, String newStatus);
    void archiveOldRequests(java.time.LocalDate cutoffDate);

    // View queries
    List<RequestSummaryViewDto> getAllRequestSummaries();
    List<RequestSummaryViewDto> getPendingRequests();
    List<RequestSummaryViewDto> getApprovedRequests();
    List<RequestSummaryViewDto> getRejectedRequests();
    List<RequestSummaryViewDto> getHighValueRequests();
    List<RequestSummaryViewDto> getRequestsReturnedForEdit();
    java.util.List<com.polatholding.procurementsystem.dto.UserRoleViewDto> getUsersWithRoles();
    java.util.List<com.polatholding.procurementsystem.dto.RecentApprovalViewDto> getRecentApprovals();
    java.util.List<com.polatholding.procurementsystem.dto.DepartmentBudgetViewDto> getDepartmentBudgets();
}
