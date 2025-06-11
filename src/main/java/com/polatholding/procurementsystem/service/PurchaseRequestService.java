package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.NewRequestFormInitDto;
import com.polatholding.procurementsystem.dto.PurchaseRequestDetailDto;
import com.polatholding.procurementsystem.dto.PurchaseRequestDto;
import com.polatholding.procurementsystem.dto.PurchaseRequestFormDto;

import java.util.List;

public interface PurchaseRequestService {

    List<PurchaseRequestDto> getRequestsForUser(String userEmail);

    NewRequestFormInitDto getNewRequestFormData(String userEmail);

    void saveNewRequest(PurchaseRequestFormDto formDto, String userEmail);

    PurchaseRequestFormDto getRequestFormById(Integer requestId);

    void updateRequest(Integer requestId, PurchaseRequestFormDto formDto, String userEmail);

    String getUserFullName(String userEmail);

    List<PurchaseRequestDto> getPendingApprovalsForUser(String userEmail);

    List<PurchaseRequestDto> getAllRequests();

    List<PurchaseRequestDto> getRequestsByBudget(Integer budgetCodeId);

    PurchaseRequestDetailDto getRequestDetailsById(Integer requestId);

    List<PurchaseRequestDto> searchUserRequests(String userEmail, String searchTerm);
}