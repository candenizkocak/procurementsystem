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
}
