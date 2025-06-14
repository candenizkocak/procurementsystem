package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.repository.DatabaseHelperRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DatabaseMaintenanceService {

    private final DatabaseHelperRepository dbHelper;

    public DatabaseMaintenanceService(DatabaseHelperRepository dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void runBackupJob() {
        dbHelper.startBackupJob();
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void archiveOldRequests() {
        java.time.LocalDate cutoff = java.time.LocalDate.now().minusYears(1);
        dbHelper.archiveOldRequests(cutoff);
    }
}
