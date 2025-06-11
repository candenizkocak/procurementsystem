package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.exception.InsufficientBudgetException;
import com.polatholding.procurementsystem.model.*;
import com.polatholding.procurementsystem.repository.*;
import com.polatholding.procurementsystem.service.RequestHistoryService;
import com.polatholding.procurementsystem.service.NotificationService;
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
    private final ApprovalStepRepository approvalStepRepository;
    private final BudgetCodeRepository budgetCodeRepository;
    private final RequestHistoryService requestHistoryService;
    private final NotificationService notificationService;

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000000");
    private static final String DIRECTOR_ROLE_NAME = "Director";
    private static final String PROCUREMENT_MANAGER_ROLE_NAME = "ProcurementManager";

    public ApprovalServiceImpl(PurchaseRequestRepository purchaseRequestRepository,
                               UserRepository userRepository,
                               ApprovalRepository approvalRepository,
                               ExchangeRateRepository exchangeRateRepository,
                               ApprovalStepRepository approvalStepRepository,
                               BudgetCodeRepository budgetCodeRepository,
                               RequestHistoryService requestHistoryService,
                               NotificationService notificationService) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.userRepository = userRepository;
        this.approvalRepository = approvalRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.budgetCodeRepository = budgetCodeRepository;
        this.requestHistoryService = requestHistoryService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void processDecision(int requestId, String userEmail, String decision, String reason) {
        User approver = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        PurchaseRequest request = purchaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Purchase Request not found: " + requestId));

        boolean isSelfApproval = approver.getUserId().equals(request.getCreatedByUser().getUserId());
        boolean isDirector = approver.getRoles().stream().anyMatch(role -> DIRECTOR_ROLE_NAME.equals(role.getRoleName()));
        if (isSelfApproval && !isDirector) {
            throw new AccessDeniedException("You cannot approve your own request.");
        }

        if ("reject".equalsIgnoreCase(decision)) {
            processRejection(request, approver, reason);
            return;
        }

        switch (request.getCurrentApprovalLevel()) {
            case 1 -> processDepartmentManagerApproval(request, approver);
            case 2 -> processProcurementManagerApproval(request, approver);
            case 3 -> processDirectorApproval(request, approver);
            default -> throw new IllegalStateException("Request is at an invalid approval level: " + request.getCurrentApprovalLevel());
        }
    }

    @Override
    @Transactional
    public void returnForEdit(int requestId, String userEmail, String comments) {
        User approver = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        PurchaseRequest request = purchaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Purchase Request not found: " + requestId));

        logApprovalAction(request, approver, "Returned for Edit", comments);

        request.setStatus("Returned for Edit");
        request.setRejectReason(comments);

        purchaseRequestRepository.save(request);
        requestHistoryService.logAction(requestId, userEmail, "Returned for Edit", comments);
        notificationService.sendAppNotification(request.getCreatedByUser().getEmail(), request.getRequestId(), "Request returned for edit.");
    }

    private void processDepartmentManagerApproval(PurchaseRequest request, User approver) {
        Integer departmentManagerId = request.getDepartment().getManagerUserId();
        if (departmentManagerId == null || !departmentManagerId.equals(approver.getUserId())) {
            throw new AccessDeniedException("You are not the designated manager for this department.");
        }
        logApprovalAction(request, approver, "Approved", null);
        request.setCurrentApprovalLevel(2);
        purchaseRequestRepository.save(request);
        notificationService.sendAppNotification(request.getCreatedByUser().getEmail(), request.getRequestId(), "Request approved by manager.");
    }

    private void processProcurementManagerApproval(PurchaseRequest request, User approver) {
        boolean isProcurementManager = approver.getRoles().stream().anyMatch(role -> PROCUREMENT_MANAGER_ROLE_NAME.equals(role.getRoleName()));
        if (!isProcurementManager) {
            throw new AccessDeniedException("User does not have the Procurement Manager role.");
        }
        logApprovalAction(request, approver, "Approved", null);
        BigDecimal valueInTRY = calculateRequestValueInTRY(request);
        if (valueInTRY.compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            request.setCurrentApprovalLevel(3);
        } else {
            consumeBudgetForRequest(request);
            request.setStatus("Approved");
            purchaseRequestRepository.save(request);
            requestHistoryService.logAction(request.getRequestId(), approver.getEmail(), "Approved", null);
            notificationService.sendAppNotification(request.getCreatedByUser().getEmail(), request.getRequestId(), "Request approved.");
            return;
        }
        purchaseRequestRepository.save(request);
        notificationService.sendAppNotification(request.getCreatedByUser().getEmail(), request.getRequestId(), "Request forwarded for director approval.");
    }

    private void processDirectorApproval(PurchaseRequest request, User approver) {
        boolean isDirector = approver.getRoles().stream().anyMatch(role -> DIRECTOR_ROLE_NAME.equals(role.getRoleName()));
        if (!isDirector) {
            throw new AccessDeniedException("User does not have the Director role.");
        }
        logApprovalAction(request, approver, "Approved", null);
        consumeBudgetForRequest(request);
        request.setStatus("Approved");
        purchaseRequestRepository.save(request);
        requestHistoryService.logAction(request.getRequestId(), approver.getEmail(), "Approved", null);
        notificationService.sendAppNotification(request.getCreatedByUser().getEmail(), request.getRequestId(), "Request approved.");
    }

    private void processRejection(PurchaseRequest request, User approver, String reason) {
        logApprovalAction(request, approver, "Rejected", reason);
        request.setStatus("Rejected");
        request.setRejectReason(reason);
        purchaseRequestRepository.save(request);
        requestHistoryService.logAction(request.getRequestId(), approver.getEmail(), "Rejected", reason);
        notificationService.sendAppNotification(request.getCreatedByUser().getEmail(), request.getRequestId(), "Request rejected.");
    }

    private void consumeBudgetForRequest(PurchaseRequest request) {
        BudgetCode budgetCode = request.getBudgetCode();
        BigDecimal requestAmountInTRY = calculateRequestValueInTRY(request);

        if (budgetCode.getBudgetAmount().compareTo(requestAmountInTRY) < 0) {
            throw new InsufficientBudgetException(
                    String.format("Approval failed: Insufficient funds in budget code '%s'. Remaining: %.2f, Required: %.2f",
                            budgetCode.getCode(),
                            budgetCode.getBudgetAmount(),
                            requestAmountInTRY)
            );
        }

        BigDecimal newBudgetAmount = budgetCode.getBudgetAmount().subtract(requestAmountInTRY);
        budgetCode.setBudgetAmount(newBudgetAmount);
        budgetCodeRepository.save(budgetCode);
    }

    private void logApprovalAction(PurchaseRequest request, User approver, String status, String reason) {
        ApprovalStep currentStep = approvalStepRepository.findByStepOrder(request.getCurrentApprovalLevel())
                .orElseThrow(() -> new IllegalStateException("Cannot log approval for a non-existent approval step level: " + request.getCurrentApprovalLevel()));

        Approval approvalLog = new Approval();
        approvalLog.setPurchaseRequest(request);
        approvalLog.setApprovalStep(currentStep);
        approvalLog.setApproverUser(approver);
        approvalLog.setApprovalStatus(status);
        approvalLog.setRejectReason(reason);
        approvalLog.setApprovalDate(LocalDateTime.now());
        approvalRepository.save(approvalLog);
    }

    private BigDecimal calculateRequestValueInTRY(PurchaseRequest request) {
        if ("TRY".equalsIgnoreCase(request.getCurrency().getCurrencyCode())) {
            return request.getNetAmount();
        }
        ExchangeRate exchangeRate = exchangeRateRepository
                .findTopByCurrencyIdAndDateLessThanEqualOrderByDateDesc(request.getCurrency().getCurrencyId(), request.getCreatedAt().toLocalDate())
                .orElseThrow(() -> new IllegalStateException("Exchange rate not found for currency code: " + request.getCurrency().getCurrencyCode()));
        return request.getNetAmount().multiply(exchangeRate.getRate());
    }
}