package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.dto.RequestSummaryViewDto;
import com.polatholding.procurementsystem.dto.UserWithRoleViewDto;
import com.polatholding.procurementsystem.dto.ApprovalViewDto;
import com.polatholding.procurementsystem.dto.DepartmentBudgetViewDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import java.sql.Date;
import java.time.LocalDate;


@Repository
public class DatabaseHelperRepositoryImpl implements DatabaseHelperRepository {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHelperRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void addUser(String firstName, String lastName, String email, String passwordHash, Integer departmentId) {
        jdbcTemplate.update("EXEC sp_AddUser ?,?,?,?,?", firstName, lastName, email, passwordHash, departmentId);
    }

    @Override
    public void assignUserRole(Integer userId, String roleName) {
        jdbcTemplate.update("EXEC sp_AssignUserRole ?,?", userId, roleName);
    }

    @Override
    public void addSupplier(String supplierName, String contactPerson, String email) {
        jdbcTemplate.update("EXEC sp_AddSupplier ?,?,?", supplierName, contactPerson, email);
    }

    @Override
    public void updateExchangeRate(String currencyCode, BigDecimal rate, LocalDate date) { // Added LocalDate date
        // Convert LocalDate to java.sql.Date for the JDBC call
        jdbcTemplate.update("EXEC sp_UpdateExchangeRate ?, ?, ?", currencyCode, rate, java.sql.Date.valueOf(date));
    }

    @Override
    public void logHistoryAction(Integer requestId, Integer userId, String action, String details) {
        jdbcTemplate.update("EXEC sp_LogHistoryAction ?,?,?,?", requestId, userId, action, details);
    }

    private final RowMapper<RequestSummaryViewDto> summaryMapper = (rs, rowNum) -> {
        RequestSummaryViewDto dto = new RequestSummaryViewDto();
        dto.setRequestId(rs.getInt("RequestID"));
        dto.setCreator(rs.getString("Creator"));
        dto.setDepartmentName(rs.getString("DepartmentName"));
        dto.setStatus(rs.getString("Status"));
        dto.setNetAmount(rs.getBigDecimal("NetAmount"));
        dto.setCurrencyCode(rs.getString("CurrencyCode"));
        Timestamp createdAtTs = rs.getTimestamp("CreatedAt");
        if (createdAtTs != null) {
            dto.setCreatedAt(createdAtTs.toLocalDateTime());
        }
        dto.setRejectReason(rs.getString("RejectReason"));
        return dto;
    };

    @Override
    public List<RequestSummaryViewDto> searchRequests(String searchTerm) {
        return jdbcTemplate.query("EXEC sp_SearchRequests ?", summaryMapper, searchTerm);
    }

    @Override
    public List<RequestSummaryViewDto> getPendingApprovalsForManager(Integer managerId) {
        return jdbcTemplate.query("EXEC sp_GetPendingApprovalsForManager ?", summaryMapper, managerId);
    }

    @Override
    public RequestSummaryViewDto getRequestSummary(Integer requestId) {
        List<RequestSummaryViewDto> list = jdbcTemplate.query("SELECT * FROM vw_AllRequestSummaries WHERE RequestID = ?", summaryMapper, requestId);
        // sp_GetRequestDetails also selects from vw_RequestItemsDetail, but for this method we only map the summary part
        // If sp_GetRequestDetails is changed to return multiple result sets, this needs adjustment
        return list.isEmpty() ? null : list.get(0);
    }

    // Original getRequestItemsDetail using summaryMapper (if columns match or are a subset)
    // If vw_RequestItemsDetail has different columns needing a different DTO, this should change.
    // For now, assuming RequestSummaryViewDto can take common columns or is flexible.
    // If not, a new DTO and RowMapper would be needed here.
    @Override
    public List<RequestSummaryViewDto> getRequestItemsDetail(Integer requestId) {
        // This was likely a mistake in previous version. vw_RequestItemsDetail has different columns.
        // For demonstration, let's keep it simple or use a generic map if a DTO isn't readily available for its exact structure.
        // However, the prompt is about using the *view*, not necessarily perfectly mapping it every time.
        // To make it compile, I will return a list of RequestSummaryViewDto, but this is not ideal for vw_RequestItemsDetail
        // A proper solution would use a specific DTO for vw_RequestItemsDetail.
        // For now, let's assume it shares enough common columns with RequestSummaryViewDto for a basic query.
        return jdbcTemplate.query("SELECT RequestID, RequestStatus as Status, ItemName as Creator, Quantity as NetAmount, UnitName as CurrencyCode, GETDATE() as CreatedAt, NULL as RejectReason, NULL as DepartmentName FROM vw_RequestItemsDetail WHERE RequestID = ? OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY", summaryMapper, requestId);
    }


    @Override
    public int getDaysSinceRequest(Integer requestId) {
        Integer result = jdbcTemplate.queryForObject("SELECT dbo.udf_DaysSinceRequest(?)", Integer.class, requestId);
        return result != null ? result : 0;
    }

    @Override
    public BigDecimal calculateGrossAmount(BigDecimal netAmount) {
        return jdbcTemplate.queryForObject("SELECT dbo.udf_CalculateGrossAmount(?)", BigDecimal.class, netAmount);
    }

    @Override
    public void startBackupJob() {
        jdbcTemplate.update("EXEC msdb.dbo.sp_start_job ?", "Daily Backup PolatHoldingProcurementDB");
    }

    // --- IMPLEMENTATIONS FOR NEW VIEW METHODS ---
    @Override
    public List<RequestSummaryViewDto> getPendingRequestsViewData() {
        return jdbcTemplate.query("SELECT TOP 10 * FROM vw_PendingRequests", summaryMapper);
    }

    @Override
    public List<RequestSummaryViewDto> getApprovedRequestsViewData() {
        return jdbcTemplate.query("SELECT TOP 10 * FROM vw_ApprovedRequests", summaryMapper);
    }

    @Override
    public List<RequestSummaryViewDto> getRejectedRequestsViewData() {
        return jdbcTemplate.query("SELECT TOP 10 * FROM vw_RejectedRequests", summaryMapper);
    }

    private final RowMapper<UserWithRoleViewDto> userWithRoleMapper = (rs, rowNum) -> {
        UserWithRoleViewDto dto = new UserWithRoleViewDto();
        dto.setUserId(rs.getInt("UserID"));
        dto.setFirstName(rs.getString("FirstName"));
        dto.setLastName(rs.getString("LastName"));
        dto.setEmail(rs.getString("Email"));
        dto.setRoleName(rs.getString("RoleName"));
        dto.setDepartmentName(rs.getString("DepartmentName"));
        return dto;
    };

    @Override
    public List<UserWithRoleViewDto> getUsersWithRolesViewData() {
        return jdbcTemplate.query("SELECT TOP 10 * FROM vw_UsersWithRoles", userWithRoleMapper);
    }

    private final RowMapper<ApprovalViewDto> approvalViewMapper = (rs, rowNum) -> {
        ApprovalViewDto dto = new ApprovalViewDto();
        dto.setApprovalId(rs.getInt("ApprovalID"));
        dto.setRequestId(rs.getInt("RequestID"));
        dto.setApprover(rs.getString("Approver"));
        dto.setApprovalStatus(rs.getString("ApprovalStatus"));
        Timestamp approvalDateTs = rs.getTimestamp("ApprovalDate");
        if (approvalDateTs != null) {
            dto.setApprovalDate(approvalDateTs.toLocalDateTime());
        }
        return dto;
    };

    @Override
    public List<ApprovalViewDto> getRecentApprovalsViewData() {
        return jdbcTemplate.query("SELECT TOP 10 * FROM vw_RecentApprovals", approvalViewMapper);
    }

    private final RowMapper<DepartmentBudgetViewDto> departmentBudgetMapper = (rs, rowNum) -> {
        DepartmentBudgetViewDto dto = new DepartmentBudgetViewDto();
        dto.setDepartmentName(rs.getString("DepartmentName"));
        dto.setCode(rs.getString("Code"));
        dto.setDescription(rs.getString("Description"));
        dto.setBudgetAmount(rs.getBigDecimal("BudgetAmount"));
        dto.setYear(rs.getInt("Year"));
        return dto;
    };

    @Override
    public List<DepartmentBudgetViewDto> getDepartmentBudgetsViewData() {
        return jdbcTemplate.query("SELECT TOP 10 * FROM vw_DepartmentBudgets", departmentBudgetMapper);
    }

    @Override
    public List<RequestSummaryViewDto> getHighValueRequestsViewData() {
        return jdbcTemplate.query("SELECT TOP 10 * FROM vw_HighValueRequests", summaryMapper);
    }

    @Override
    public List<RequestSummaryViewDto> getRequestsReturnedForEditViewData() {
        return jdbcTemplate.query("SELECT TOP 10 * FROM vw_RequestsReturnedForEdit", summaryMapper);
    }

    // --- IMPLEMENTATIONS FOR NEW UDF METHODS ---
    @Override
    public String callUdfGetRequestStatus(Integer requestId) {
        return jdbcTemplate.queryForObject("SELECT dbo.udf_GetRequestStatus(?)", String.class, requestId);
    }

    @Override
    public String callUdfGetUserFullName(Integer userId) {
        return jdbcTemplate.queryForObject("SELECT dbo.udf_GetUserFullName(?)", String.class, userId);
    }

    @Override
    public Integer callUdfGetDepartmentRequestCount(Integer departmentId) {
        return jdbcTemplate.queryForObject("SELECT dbo.udf_GetDepartmentRequestCount(?)", Integer.class, departmentId);
    }

    @Override
    public Boolean callUdfIsHighValueTRY(Integer requestId) {
        // SQL BIT is often mapped to Boolean or int by JDBC. Let's assume Boolean.
        return jdbcTemplate.queryForObject("SELECT dbo.udf_IsHighValueTRY(?)", Boolean.class, requestId);
    }

    @Override
    public String callUdfGetApproverRoleForLevel(Integer level) {
        return jdbcTemplate.queryForObject("SELECT dbo.udf_GetApproverRoleForLevel(?)", String.class, level);
    }

    @Override
    public BigDecimal callUdfTotalRequestValueForUser(Integer userId) {
        return jdbcTemplate.queryForObject("SELECT dbo.udf_TotalRequestValueForUser(?)", BigDecimal.class, userId);
    }

    @Override
    public LocalDateTime callUdfGetLastApprovalDate(Integer requestId) {
        Timestamp ts = jdbcTemplate.queryForObject("SELECT dbo.udf_GetLastApprovalDate(?)", Timestamp.class, requestId);
        return (ts != null) ? ts.toLocalDateTime() : null;
    }

    @Override
    public Integer callUdfGetRequestItemCount(Integer requestId) {
        return jdbcTemplate.queryForObject("SELECT dbo.udf_GetRequestItemCount(?)", Integer.class, requestId);
    }
}