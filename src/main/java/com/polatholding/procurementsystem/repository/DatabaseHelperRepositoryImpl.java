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
}
