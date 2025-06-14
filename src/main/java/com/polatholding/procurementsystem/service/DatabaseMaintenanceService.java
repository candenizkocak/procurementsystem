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
}
