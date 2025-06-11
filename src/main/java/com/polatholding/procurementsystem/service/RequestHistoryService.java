package com.polatholding.procurementsystem.service;

public interface RequestHistoryService {
    void logAction(int requestId, String userEmail, String action, String details);
}
