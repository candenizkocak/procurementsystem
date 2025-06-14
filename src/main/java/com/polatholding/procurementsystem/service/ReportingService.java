package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.repository.DatabaseHelperRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ReportingService {
    private final DatabaseHelperRepository dbHelper;

    public ReportingService(DatabaseHelperRepository dbHelper) {
        this.dbHelper = dbHelper;
    }

    // Runs daily to demonstrate usage of views and UDFs
    @Scheduled(cron = "0 0 4 * * ?")
    public void collectStats() {
        dbHelper.getAllRequestSummaries().size();
        dbHelper.getPendingRequests().size();
        dbHelper.getApprovedRequests().size();
        dbHelper.getRejectedRequests().size();
        dbHelper.getHighValueRequests().size();
        dbHelper.getRequestsReturnedForEdit().size();
        dbHelper.getUsersWithRoles().size();
        dbHelper.getRecentApprovals().size();
        dbHelper.getDepartmentBudgets().size();
    }
}
