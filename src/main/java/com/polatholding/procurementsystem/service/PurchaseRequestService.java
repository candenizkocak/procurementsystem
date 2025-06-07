package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.NewRequestFormInitDto;
import com.polatholding.procurementsystem.dto.PurchaseRequestDetailDto;
import com.polatholding.procurementsystem.dto.PurchaseRequestDto;
import com.polatholding.procurementsystem.dto.PurchaseRequestFormDto;

import java.util.List;

public interface PurchaseRequestService {

    // The primary method for displaying requests on the dashboard.
    List<PurchaseRequestDto> getRequestsForUser(String userEmail);

    NewRequestFormInitDto getNewRequestFormData();

    void saveNewRequest(PurchaseRequestFormDto formDto, String userEmail);

    List<PurchaseRequestDto> getPendingApprovalsForUser(String userEmail);

    // This method is now effectively unused but required by the interface contract.
    List<PurchaseRequestDto> getAllRequests();

    PurchaseRequestDetailDto getRequestDetailsById(Integer requestId);
}