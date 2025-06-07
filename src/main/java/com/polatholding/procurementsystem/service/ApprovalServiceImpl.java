package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.model.*;
import com.polatholding.procurementsystem.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ApprovalServiceImpl implements ApprovalService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;
    private final ApprovalRepository approvalRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ApprovalStepRepository approvalStepRepository; // <-- RE-ADD THIS REPOSITORY

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000000");
    private static final String DIRECTOR_ROLE_NAME = "Director";
    private static final String PROCUREMENT_MANAGER_ROLE_NAME = "ProcurementManager";

    public ApprovalServiceImpl(PurchaseRequestRepository purchaseRequestRepository,
                               UserRepository userRepository,
                               ApprovalRepository approvalRepository,
                               ExchangeRateRepository exchangeRateRepository,
                               ApprovalStepRepository approvalStepRepository) { // <-- ADD TO CONSTRUCTOR
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.userRepository = userRepository;
        this.approvalRepository = approvalRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.approvalStepRepository = approvalStepRepository; // <-- INITIALIZE
    }

    @Override
    @Transactional
    public void processDecision(int requestId, String userEmail, String decision, String reason) {
        // 1. Fetch entities
        User approver = userRepository.findByEmail(userEmail).orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        PurchaseRequest request = purchaseRequestRepository.findById(requestId).orElseThrow(() -> new RuntimeException("Purchase Request not found: " + requestId));

        // 2. Self-Approval Check
        boolean isSelfApproval = approver.getUserId().equals(request.getCreatedByUser().getUserId());
        boolean isDirector = approver.getRoles().stream().anyMatch(role -> DIRECTOR_ROLE_NAME.equals(role.getRoleName()));
        if (isSelfApproval && !isDirector) {
            throw new AccessDeniedException("You cannot approve your own request.");
        }

        // 3. Process Rejection
        if ("reject".equalsIgnoreCase(decision)) {
            processRejection(request, approver, reason);
            return;
        }

        // 4. Process Approval based on the current level
        switch (request.getCurrentApprovalLevel()) {
            case 1:
                processDepartmentManagerApproval(request, approver);
                break;
            case 2:
                processProcurementManagerApproval(request, approver);
                break;
            case 3:
                processDirectorApproval(request, approver);
                break;
            default:
                throw new IllegalStateException("Request is at an invalid approval level: " + request.getCurrentApprovalLevel());
        }
    }

    private void processDepartmentManagerApproval(PurchaseRequest request, User approver) {
        Integer departmentManagerId = request.getDepartment().getManagerUserId();
        if (departmentManagerId == null || !departmentManagerId.equals(approver.getUserId())) {
            throw new AccessDeniedException("You are not the designated manager for this department.");
        }
        request.setCurrentApprovalLevel(2);
        logApproval(request, approver, "Approved", null);
    }

    private void processProcurementManagerApproval(PurchaseRequest request, User approver) {
        boolean isProcurementManager = approver.getRoles().stream().anyMatch(role -> PROCUREMENT_MANAGER_ROLE_NAME.equals(role.getRoleName()));
        if (!isProcurementManager) {
            throw new AccessDeniedException("User does not have the Procurement Manager role.");
        }
        BigDecimal valueInTRY = calculateRequestValueInTRY(request);
        if (valueInTRY.compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            request.setCurrentApprovalLevel(3);
        } else {
            request.setStatus("Approved");
        }
        logApproval(request, approver, "Approved", null);
    }

    private void processDirectorApproval(PurchaseRequest request, User approver) {
        boolean isDirector = approver.getRoles().stream().anyMatch(role -> DIRECTOR_ROLE_NAME.equals(role.getRoleName()));
        if (!isDirector) {
            throw new AccessDeniedException("User does not have the Director role.");
        }
        request.setStatus("Approved");
        logApproval(request, approver, "Approved", null);
    }

    private void processRejection(PurchaseRequest request, User approver, String reason) {
        request.setStatus("Rejected");
        request.setRejectReason(reason);
        logApproval(request, approver, "Rejected", reason);
    }

    // *** FIX FOR BUG #1: CORRECTED LOGGING METHOD ***
    private void logApproval(PurchaseRequest request, User approver, String status, String reason) {
        // Find the ApprovalStep that corresponds to the request's CURRENT level before it changes
        ApprovalStep currentStep = approvalStepRepository.findByStepOrder(request.getCurrentApprovalLevel())
                .orElseThrow(() -> new IllegalStateException("Cannot log approval for a non-existent approval step level: " + request.getCurrentApprovalLevel()));

        Approval approvalLog = new Approval();
        approvalLog.setPurchaseRequest(request);
        approvalLog.setApprovalStep(currentStep); // <-- SET THE APPROVAL STEP
        approvalLog.setApproverUser(approver);
        approvalLog.setApprovalStatus(status);
        approvalLog.setRejectReason(reason);
        approvalLog.setApprovalDate(LocalDateTime.now());
        approvalRepository.save(approvalLog);
    }
    // ***********************************************

    private BigDecimal calculateRequestValueInTRY(PurchaseRequest request) {
        if ("TRY".equalsIgnoreCase(request.getCurrency().getCurrencyCode())) return request.getNetAmount();
        ExchangeRate exchangeRate = exchangeRateRepository
                .findTopByCurrencyIdAndDateLessThanEqualOrderByDateDesc(request.getCurrency().getCurrencyId(), request.getCreatedAt().toLocalDate())
                .orElseThrow(() -> new RuntimeException("Exchange rate not found for currency."));
        return request.getNetAmount().multiply(exchangeRate.getRate());
    }
}