package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.dto.RequestSummaryViewDto;
import com.polatholding.procurementsystem.dto.UserWithRoleViewDto;
import com.polatholding.procurementsystem.dto.ApprovalViewDto;
import com.polatholding.procurementsystem.dto.DepartmentBudgetViewDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public interface DatabaseHelperRepository {
    void addUser(String firstName, String lastName, String email, String passwordHash, Integer departmentId);
    void assignUserRole(Integer userId, String roleName);
    void addSupplier(String supplierName, String contactPerson, String email);
    void updateExchangeRate(String currencyCode, BigDecimal rate, LocalDate date);
    void logHistoryAction(Integer requestId, Integer userId, String action, String details);
    List<RequestSummaryViewDto> searchRequests(String searchTerm);
    List<RequestSummaryViewDto> getPendingApprovalsForManager(Integer managerId);
    RequestSummaryViewDto getRequestSummary(Integer requestId); // Uses vw_AllRequestSummaries via sp_GetRequestDetails
    List<RequestSummaryViewDto> getRequestItemsDetail(Integer requestId); // Uses vw_RequestItemsDetail
    int getDaysSinceRequest(Integer requestId); // UDF
    BigDecimal calculateGrossAmount(BigDecimal netAmount); // UDF
    void startBackupJob();

    // --- NEW METHODS FOR DISTINCT VIEW USAGE ---
    List<RequestSummaryViewDto> getPendingRequestsViewData();       // vw_PendingRequests
    List<RequestSummaryViewDto> getApprovedRequestsViewData();      // vw_ApprovedRequests
    List<RequestSummaryViewDto> getRejectedRequestsViewData();      // vw_RejectedRequests
    List<UserWithRoleViewDto> getUsersWithRolesViewData();        // vw_UsersWithRoles
    List<ApprovalViewDto> getRecentApprovalsViewData();         // vw_RecentApprovals
    List<DepartmentBudgetViewDto> getDepartmentBudgetsViewData();   // vw_DepartmentBudgets
    List<RequestSummaryViewDto> getHighValueRequestsViewData();     // vw_HighValueRequests
    List<RequestSummaryViewDto> getRequestsReturnedForEditViewData(); // vw_RequestsReturnedForEdit

    // --- NEW METHODS FOR DISTINCT UDF USAGE ---
    String callUdfGetRequestStatus(Integer requestId);
    String callUdfGetUserFullName(Integer userId);
    Integer callUdfGetDepartmentRequestCount(Integer departmentId);
    Boolean callUdfIsHighValueTRY(Integer requestId);
    String callUdfGetApproverRoleForLevel(Integer level);
    BigDecimal callUdfTotalRequestValueForUser(Integer userId);
    java.time.LocalDateTime callUdfGetLastApprovalDate(Integer requestId);
    Integer callUdfGetRequestItemCount(Integer requestId);
}