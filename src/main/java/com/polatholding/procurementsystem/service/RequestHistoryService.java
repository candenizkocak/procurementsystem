package com.polatholding.procurementsystem.service;

public interface RequestHistoryService {
    void logAction(int requestId, String userEmail, String action, String details);

    java.util.List<com.polatholding.procurementsystem.dto.RequestHistoryDto> getAllHistory();
}
