package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.*;
import com.polatholding.procurementsystem.repository.DatabaseHelperRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UtilityServiceImpl implements UtilityService {

    private static final Logger log = LoggerFactory.getLogger(UtilityServiceImpl.class);
    private final DatabaseHelperRepository dbHelper;

    public UtilityServiceImpl(DatabaseHelperRepository dbHelper) {
        this.dbHelper = dbHelper;
    }

    @PostConstruct
    public void init() {
        log.info("UtilityService: Running distinct database object usage demonstration on startup.");
        demonstrateDistinctDatabaseObjectUsage();
        log.info("UtilityService: Demonstration complete.");
    }

    @Override
    public void demonstrateDistinctDatabaseObjectUsage() {
        log.info("--- Demonstrating Distinct View Usage ---");
        try {
            List<RequestSummaryViewDto> pending = dbHelper.getPendingRequestsViewData();
            log.info("vw_PendingRequests: Fetched {} records. First: {}", pending.size(), pending.isEmpty() ? "N/A" : pending.get(0).getRequestId());

            List<RequestSummaryViewDto> approved = dbHelper.getApprovedRequestsViewData();
            log.info("vw_ApprovedRequests: Fetched {} records. First: {}", approved.size(), approved.isEmpty() ? "N/A" : approved.get(0).getRequestId());

            List<RequestSummaryViewDto> rejected = dbHelper.getRejectedRequestsViewData();
            log.info("vw_RejectedRequests: Fetched {} records. First: {}", rejected.size(), rejected.isEmpty() ? "N/A" : rejected.get(0).getRequestId());

            List<UserWithRoleViewDto> usersWithRoles = dbHelper.getUsersWithRolesViewData();
            log.info("vw_UsersWithRoles: Fetched {} records. First: {}", usersWithRoles.size(), usersWithRoles.isEmpty() ? "N/A" : usersWithRoles.get(0).getEmail());

            List<ApprovalViewDto> recentApprovals = dbHelper.getRecentApprovalsViewData();
            log.info("vw_RecentApprovals: Fetched {} records. First: {}", recentApprovals.size(), recentApprovals.isEmpty() ? "N/A" : recentApprovals.get(0).getApprovalId());

            List<DepartmentBudgetViewDto> deptBudgets = dbHelper.getDepartmentBudgetsViewData();
            log.info("vw_DepartmentBudgets: Fetched {} records. First: {}", deptBudgets.size(), deptBudgets.isEmpty() ? "N/A" : deptBudgets.get(0).getCode());

            List<RequestSummaryViewDto> highValue = dbHelper.getHighValueRequestsViewData();
            log.info("vw_HighValueRequests: Fetched {} records. First: {}", highValue.size(), highValue.isEmpty() ? "N/A" : highValue.get(0).getRequestId());

            List<RequestSummaryViewDto> returnedForEdit = dbHelper.getRequestsReturnedForEditViewData();
            log.info("vw_RequestsReturnedForEdit: Fetched {} records. First: {}", returnedForEdit.size(), returnedForEdit.isEmpty() ? "N/A" : returnedForEdit.get(0).getRequestId());

            // The original 2 views used via SPs/direct calls
            RequestSummaryViewDto summary = dbHelper.getRequestSummary(1); // Assuming RequestID 1 exists
            log.info("vw_AllRequestSummaries (via getRequestSummary for ID 1): {}", summary != null ? summary.getStatus() : "N/A - Request 1 not found");
            List<RequestSummaryViewDto> itemsDetail = dbHelper.getRequestItemsDetail(1); // Assuming RequestID 1 exists
            log.info("vw_RequestItemsDetail (for ID 1): Fetched {} pseudo-item records.", itemsDetail.size());


        } catch (Exception e) {
            log.error("Error during View usage demonstration: {}", e.getMessage());
        }

        log.info("--- Demonstrating Distinct UDF Usage ---");
        try {
            // Sample IDs - these might fail if data doesn't exist, but it shows the call.
            // For robust demo, these IDs should be fetched or known to exist.
            Integer sampleRequestId = 1;
            Integer sampleUserId = 1;
            Integer sampleDepartmentId = 1;
            Integer sampleApprovalLevel = 1;

            // UDFs already used:
            // udf_DaysSinceRequest (via getDaysSinceRequest)
            // udf_CalculateGrossAmount (via calculateGrossAmount)
            // For the demo, we call them again here to be explicit if needed, or rely on existing calls.
            // dbHelper.getDaysSinceRequest(sampleRequestId);
            // dbHelper.calculateGrossAmount(BigDecimal.TEN);

            String status = dbHelper.callUdfGetRequestStatus(sampleRequestId);
            log.info("udf_GetRequestStatus (Request ID {}): {}", sampleRequestId, status);

            String fullName = dbHelper.callUdfGetUserFullName(sampleUserId);
            log.info("udf_GetUserFullName (User ID {}): {}", sampleUserId, fullName);

            Integer deptReqCount = dbHelper.callUdfGetDepartmentRequestCount(sampleDepartmentId);
            log.info("udf_GetDepartmentRequestCount (Dept ID {}): {}", sampleDepartmentId, deptReqCount);

            Boolean isHighVal = dbHelper.callUdfIsHighValueTRY(sampleRequestId);
            log.info("udf_IsHighValueTRY (Request ID {}): {}", sampleRequestId, isHighVal);

            String approverRole = dbHelper.callUdfGetApproverRoleForLevel(sampleApprovalLevel);
            log.info("udf_GetApproverRoleForLevel (Level {}): {}", sampleApprovalLevel, approverRole);

            BigDecimal totalUserValue = dbHelper.callUdfTotalRequestValueForUser(sampleUserId);
            log.info("udf_TotalRequestValueForUser (User ID {}): {}", sampleUserId, totalUserValue);

            LocalDateTime lastApprovalDate = dbHelper.callUdfGetLastApprovalDate(sampleRequestId);
            log.info("udf_GetLastApprovalDate (Request ID {}): {}", sampleRequestId, lastApprovalDate);

            Integer itemCount = dbHelper.callUdfGetRequestItemCount(sampleRequestId);
            log.info("udf_GetRequestItemCount (Request ID {}): {}", sampleRequestId, itemCount);

        } catch (Exception e) {
            log.error("Error during UDF usage demonstration (sample data might be missing for IDs 1): {}", e.getMessage());
        }
    }
}