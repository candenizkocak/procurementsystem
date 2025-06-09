package com.polatholding.procurementsystem.service;

public interface ApprovalService {
    void processDecision(int requestId, String userEmail, String decision, String reason);
    void returnForEdit(int requestId, String userEmail, String comments);
}