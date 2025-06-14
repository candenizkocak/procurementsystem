package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.dto.RequestSummaryViewDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
    public void updateExchangeRate(String currencyCode, BigDecimal rate) {
        jdbcTemplate.update("EXEC sp_UpdateExchangeRate ?,?", currencyCode, rate);
    }

    @Override
    public void logHistoryAction(Integer requestId, Integer userId, String action, String details) {
        jdbcTemplate.update("EXEC sp_LogHistoryAction ?,?,?,?", requestId, userId, action, details);
    }

    private final RowMapper<RequestSummaryViewDto> summaryMapper = new RowMapper<>() {
        @Override
        public RequestSummaryViewDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            RequestSummaryViewDto dto = new RequestSummaryViewDto();
            dto.setRequestId(rs.getInt("RequestID"));
            dto.setCreator(rs.getString("Creator"));
            dto.setDepartmentName(rs.getString("DepartmentName"));
            dto.setStatus(rs.getString("Status"));
            dto.setNetAmount(rs.getBigDecimal("NetAmount"));
            dto.setCurrencyCode(rs.getString("CurrencyCode"));
            dto.setCreatedAt(rs.getTimestamp("CreatedAt").toLocalDateTime());
            dto.setRejectReason(rs.getString("RejectReason"));
            return dto;
        }
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
        List<RequestSummaryViewDto> list = jdbcTemplate.query("EXEC sp_GetRequestDetails ?", summaryMapper, requestId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<RequestSummaryViewDto> getRequestItemsDetail(Integer requestId) {
        return jdbcTemplate.query("SELECT * FROM vw_RequestItemsDetail WHERE RequestID = ?", summaryMapper, requestId);
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

    @Override
    public String getRequestStatus(Integer requestId) {
        return jdbcTemplate.queryForObject("SELECT dbo.udf_GetRequestStatus(?)", String.class, requestId);
    }

    @Override
    public String getUserFullNameById(Integer userId) {
        return jdbcTemplate.queryForObject("SELECT dbo.udf_GetUserFullName(?)", String.class, userId);
    }

    @Override
    public int getDepartmentRequestCount(Integer departmentId) {
        Integer result = jdbcTemplate.queryForObject("SELECT dbo.udf_GetDepartmentRequestCount(?)", Integer.class, departmentId);
        return result != null ? result : 0;
    }

    @Override
    public boolean isHighValueTRY(Integer requestId) {
        Integer result = jdbcTemplate.queryForObject("SELECT dbo.udf_IsHighValueTRY(?)", Integer.class, requestId);
        return result != null && result == 1;
    }

    @Override
    public String getApproverRoleForLevel(Integer level) {
        return jdbcTemplate.queryForObject("SELECT dbo.udf_GetApproverRoleForLevel(?)", String.class, level);
    }

    @Override
    public BigDecimal totalRequestValueForUser(Integer userId) {
        return jdbcTemplate.queryForObject("SELECT dbo.udf_TotalRequestValueForUser(?)", BigDecimal.class, userId);
    }

    @Override
    public java.time.LocalDateTime getLastApprovalDate(Integer requestId) {
        return jdbcTemplate.queryForObject("SELECT dbo.udf_GetLastApprovalDate(?)", java.time.LocalDateTime.class, requestId);
    }

    @Override
    public int getRequestItemCount(Integer requestId) {
        Integer result = jdbcTemplate.queryForObject("SELECT dbo.udf_GetRequestItemCount(?)", Integer.class, requestId);
        return result != null ? result : 0;
    }

    @Override
    public void updateRequestStatus(Integer requestId, String newStatus) {
        jdbcTemplate.update("EXEC sp_UpdateRequestStatus ?,?", requestId, newStatus);
    }

    @Override
    public void archiveOldRequests(java.time.LocalDate cutoffDate) {
        jdbcTemplate.update("EXEC sp_ArchiveOldRequests ?", java.sql.Date.valueOf(cutoffDate));
    }

    @Override
    public List<RequestSummaryViewDto> getAllRequestSummaries() {
        return jdbcTemplate.query("SELECT * FROM vw_AllRequestSummaries", summaryMapper);
    }

    @Override
    public List<RequestSummaryViewDto> getPendingRequests() {
        return jdbcTemplate.query("SELECT * FROM vw_PendingRequests", summaryMapper);
    }

    @Override
    public List<RequestSummaryViewDto> getApprovedRequests() {
        return jdbcTemplate.query("SELECT * FROM vw_ApprovedRequests", summaryMapper);
    }

    @Override
    public List<RequestSummaryViewDto> getRejectedRequests() {
        return jdbcTemplate.query("SELECT * FROM vw_RejectedRequests", summaryMapper);
    }

    @Override
    public List<RequestSummaryViewDto> getHighValueRequests() {
        return jdbcTemplate.query("SELECT * FROM vw_HighValueRequests", summaryMapper);
    }

    @Override
    public List<RequestSummaryViewDto> getRequestsReturnedForEdit() {
        return jdbcTemplate.query("SELECT * FROM vw_RequestsReturnedForEdit", summaryMapper);
    }

    private final RowMapper<com.polatholding.procurementsystem.dto.UserRoleViewDto> userRoleMapper = new RowMapper<>() {
        @Override
        public com.polatholding.procurementsystem.dto.UserRoleViewDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            com.polatholding.procurementsystem.dto.UserRoleViewDto dto = new com.polatholding.procurementsystem.dto.UserRoleViewDto();
            dto.setUserId(rs.getInt("UserID"));
            dto.setFirstName(rs.getString("FirstName"));
            dto.setLastName(rs.getString("LastName"));
            dto.setEmail(rs.getString("Email"));
            dto.setRoleName(rs.getString("RoleName"));
            dto.setDepartmentName(rs.getString("DepartmentName"));
            return dto;
        }
    };

    @Override
    public List<com.polatholding.procurementsystem.dto.UserRoleViewDto> getUsersWithRoles() {
        return jdbcTemplate.query("SELECT * FROM vw_UsersWithRoles", userRoleMapper);
    }

    private final RowMapper<com.polatholding.procurementsystem.dto.RecentApprovalViewDto> approvalMapper = new RowMapper<>() {
        @Override
        public com.polatholding.procurementsystem.dto.RecentApprovalViewDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            com.polatholding.procurementsystem.dto.RecentApprovalViewDto dto = new com.polatholding.procurementsystem.dto.RecentApprovalViewDto();
            dto.setApprovalId(rs.getInt("ApprovalID"));
            dto.setRequestId(rs.getInt("RequestID"));
            dto.setApprover(rs.getString("Approver"));
            dto.setApprovalStatus(rs.getString("ApprovalStatus"));
            dto.setApprovalDate(rs.getTimestamp("ApprovalDate").toLocalDateTime());
            return dto;
        }
    };

    @Override
    public List<com.polatholding.procurementsystem.dto.RecentApprovalViewDto> getRecentApprovals() {
        return jdbcTemplate.query("SELECT * FROM vw_RecentApprovals", approvalMapper);
    }

    private final RowMapper<com.polatholding.procurementsystem.dto.DepartmentBudgetViewDto> budgetMapper = new RowMapper<>() {
        @Override
        public com.polatholding.procurementsystem.dto.DepartmentBudgetViewDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            com.polatholding.procurementsystem.dto.DepartmentBudgetViewDto dto = new com.polatholding.procurementsystem.dto.DepartmentBudgetViewDto();
            dto.setDepartmentName(rs.getString("DepartmentName"));
            dto.setCode(rs.getString("Code"));
            dto.setDescription(rs.getString("Description"));
            dto.setBudgetAmount(rs.getBigDecimal("BudgetAmount"));
            dto.setYear(rs.getInt("Year"));
            return dto;
        }
    };

    @Override
    public List<com.polatholding.procurementsystem.dto.DepartmentBudgetViewDto> getDepartmentBudgets() {
        return jdbcTemplate.query("SELECT * FROM vw_DepartmentBudgets", budgetMapper);
    }
}
