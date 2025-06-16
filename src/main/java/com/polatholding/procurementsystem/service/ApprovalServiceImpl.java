package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.exception.InsufficientBudgetException;
import com.polatholding.procurementsystem.model.*;
import com.polatholding.procurementsystem.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class ApprovalServiceImpl implements ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalServiceImpl.class);

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;
    private final ApprovalRepository approvalRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final BudgetCodeRepository budgetCodeRepository;
    private final RequestHistoryService requestHistoryService;
    private final NotificationService notificationService; // Added

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000000");
    private static final String DIRECTOR_ROLE_NAME = "Director";
    private static final String PROCUREMENT_MANAGER_ROLE_NAME = "ProcurementManager";
    private static final String MANAGER_ROLE_NAME = "Manager"; // For checking department manager

    public ApprovalServiceImpl(PurchaseRequestRepository purchaseRequestRepository,
                               UserRepository userRepository,
                               ApprovalRepository approvalRepository,
                               ExchangeRateRepository exchangeRateRepository,
                               ApprovalStepRepository approvalStepRepository,
                               BudgetCodeRepository budgetCodeRepository,
                               RequestHistoryService requestHistoryService,
                               NotificationService notificationService) { // Added notificationService
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.userRepository = userRepository;
        this.approvalRepository = approvalRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.budgetCodeRepository = budgetCodeRepository;
        this.requestHistoryService = requestHistoryService;
        this.notificationService = notificationService; // Added assignment
    }

    @Override
    @Transactional
    public void processDecision(int requestId, String userEmail, String decision, String reason) {
        User approver = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        PurchaseRequest request = purchaseRequestRepository.findByIdWithAllDetails(requestId)
                .orElseThrow(() -> new RuntimeException("Purchase Request not found: " + requestId));

        // Ensure creator and department manager are loaded for notification/logic
        if (request.getCreatedByUser() == null) {
            request.setCreatedByUser(userRepository.findById(request.getCreatedByUser().getUserId()).orElseThrow());
        }
        if (request.getDepartment() != null && request.getDepartment().getManagerUser() == null && request.getDepartment().getManagerUserId() != null) {
            userRepository.findById(request.getDepartment().getManagerUserId()).ifPresent(manager -> request.getDepartment().setManagerUser(manager));
        }


        boolean isSelfApproval = approver.getUserId().equals(request.getCreatedByUser().getUserId());
        boolean isDirectorActing = approver.getRoles().stream().anyMatch(role -> DIRECTOR_ROLE_NAME.equals(role.getRoleName()));
        if (isSelfApproval && !isDirectorActing) { // Directors can "approve" their own technically by it being auto-approved or them being the final step
            // This check is more for a standard employee/manager trying to approve their own request at a lower level
            if(request.getCurrentApprovalLevel() == 1 && approver.getUserId().equals(request.getDepartment().getManagerUserId())){
                // A manager is approving their own request which is at level 1. This means it should go to level 2.
                // This is not a "denied" self-approval, but a progression.
                log.info("Manager UserID {} is 'approving' their own RequestID {} at Level 1. Progressing to Level 2.", approver.getUserId(), requestId);
            } else {
                throw new AccessDeniedException("You cannot approve your own request at this stage unless you are a Director acting as final approver.");
            }
        }


        String statusBeforeProcessing = request.getStatus();
        int levelBeforeProcessing = request.getCurrentApprovalLevel();

        if ("reject".equalsIgnoreCase(decision)) {
            processRejection(request, approver, reason);
            notificationService.notifyRejection(request, approver);
            return;
        }

        // --- Approval Logic ---
        switch (request.getCurrentApprovalLevel()) {
            case 1: // Department Manager
                processDepartmentManagerApproval(request, approver);
                break;
            case 2: // Procurement Manager
                processProcurementManagerApproval(request, approver);
                break;
            case 3: // Director
                processDirectorApproval(request, approver);
                break;
            default:
                log.error("Request {} is at an invalid approval level: {}", requestId, request.getCurrentApprovalLevel());
                throw new IllegalStateException("Request is at an invalid approval level: " + request.getCurrentApprovalLevel());
        }

        // --- Notification Logic ---
        if ("Approved".equalsIgnoreCase(request.getStatus()) && !"Approved".equalsIgnoreCase(statusBeforeProcessing)) {
            // Final approval occurred in this step
            notificationService.notifyFinalApproval(request, approver);
        } else if ("Pending".equalsIgnoreCase(request.getStatus()) && request.getCurrentApprovalLevel() > levelBeforeProcessing) {
            // Moved to the next pending step
            notificationService.notifyApprovalStep(request, approver, statusBeforeProcessing);
        } else if (isSelfApproval && request.getCurrentApprovalLevel() > levelBeforeProcessing) {
            // Specific case for manager approving own request, which then moves to next stage
            log.info("Manager {} approved their own request {}, which moved to level {}. Notifying next approvers.", approver.getEmail(), request.getRequestId(), request.getCurrentApprovalLevel());
            notificationService.notifyApprovalStep(request, approver, statusBeforeProcessing);
        }
    }


    private void processDepartmentManagerApproval(PurchaseRequest request, User approver) {
        // Ensure department and its manager are loaded
        Department requestDepartment = request.getDepartment();
        if (requestDepartment == null) throw new IllegalStateException("Request department is null for RequestID: " + request.getRequestId());

        User deptManager = requestDepartment.getManagerUser();
        if (deptManager == null && requestDepartment.getManagerUserId() != null) {
            deptManager = userRepository.findById(requestDepartment.getManagerUserId()).orElse(null);
        }

        if (deptManager == null || !deptManager.getUserId().equals(approver.getUserId())) {
            // Check if approver is a generic "Manager" role and also the designated manager for *this* department
            boolean isManagerRole = approver.getRoles().stream().anyMatch(r -> MANAGER_ROLE_NAME.equals(r.getRoleName()));
            if (!isManagerRole || deptManager == null || !deptManager.getUserId().equals(approver.getUserId())) {
                log.warn("Access Denied: User {} (ID: {}) is not the designated manager for department {} (ManagerID: {}) for request {}",
                        approver.getEmail(), approver.getUserId(),
                        requestDepartment.getDepartmentName(), requestDepartment.getManagerUserId(), request.getRequestId());
                throw new AccessDeniedException("You are not the designated manager for this department or do not have the Manager role.");
            }
        }

        logApprovalAction(request, approver, "Approved (Dept. Mgr)", null);
        request.setCurrentApprovalLevel(2); // Move to Procurement Manager
        request.setStatus("Pending"); // Still pending overall
        purchaseRequestRepository.save(request);
        requestHistoryService.logAction(request.getRequestId(), approver.getEmail(), "Approved (Dept. Mgr)", "Department Manager approved.");
    }

    private void processProcurementManagerApproval(PurchaseRequest request, User approver) {
        boolean isProcurementManager = approver.getRoles().stream().anyMatch(role -> PROCUREMENT_MANAGER_ROLE_NAME.equals(role.getRoleName()));
        if (!isProcurementManager) {
            throw new AccessDeniedException("User does not have the Procurement Manager role.");
        }
        logApprovalAction(request, approver, "Approved (Proc. Mgr)", null);

        BigDecimal valueInTRY = calculateRequestValueInTRY(request);
        if (valueInTRY.compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            request.setCurrentApprovalLevel(3); // Move to Director
            request.setStatus("Pending");
        } else {
            consumeBudgetForRequest(request); // Final approval for < threshold
            request.setStatus("Approved");
            request.setCurrentApprovalLevel(99); // Mark as fully processed for approval levels
        }
        purchaseRequestRepository.save(request);
        requestHistoryService.logAction(request.getRequestId(), approver.getEmail(), "Approved (Proc. Mgr)", "Procurement Manager approved.");
    }

    private void processDirectorApproval(PurchaseRequest request, User approver) {
        boolean isDirector = approver.getRoles().stream().anyMatch(role -> DIRECTOR_ROLE_NAME.equals(role.getRoleName()));
        if (!isDirector) {
            throw new AccessDeniedException("User does not have the Director role.");
        }
        logApprovalAction(request, approver, "Approved (Director)", null);
        consumeBudgetForRequest(request); // Final approval
        request.setStatus("Approved");
        request.setCurrentApprovalLevel(99); // Mark as fully processed
        purchaseRequestRepository.save(request);
        requestHistoryService.logAction(request.getRequestId(), approver.getEmail(), "Approved (Director)", "Director approved.");
    }

    private void processRejection(PurchaseRequest request, User approver, String reason) {
        logApprovalAction(request, approver, "Rejected", reason);
        request.setStatus("Rejected");
        request.setRejectReason(reason);
        // CurrentApprovalLevel might be kept as is, or reset, depending on business rule.
        // For rejection, it typically stops.
        purchaseRequestRepository.save(request);
        requestHistoryService.logAction(request.getRequestId(), approver.getEmail(), "Rejected", reason);
    }

    @Override
    @Transactional
    public void returnForEdit(int requestId, String userEmail, String comments) {
        User approver = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        PurchaseRequest request = purchaseRequestRepository.findByIdWithAllDetails(requestId)
                .orElseThrow(() -> new RuntimeException("Purchase Request not found: " + requestId));

        // Ensure creator is available for notification
        if (request.getCreatedByUser() == null) {
            request.setCreatedByUser(userRepository.findById(request.getCreatedByUser().getUserId()).orElseThrow());
        }

        logApprovalAction(request, approver, "Returned for Edit", comments);

        request.setStatus("Returned for Edit");
        request.setRejectReason(comments); // Using rejectReason for comments of return

        purchaseRequestRepository.save(request);
        requestHistoryService.logAction(requestId, userEmail, "Returned for Edit", comments);
        notificationService.notifyReturnForEdit(request, approver);
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
        log.info("Budget consumed for RequestID: {}. BudgetCode: {}, Consumed: {}, Remaining: {}",
                request.getRequestId(), budgetCode.getCode(), requestAmountInTRY, newBudgetAmount);
    }

    private void logApprovalAction(PurchaseRequest request, User approver, String status, String reason) {
        ApprovalStep currentStep = approvalStepRepository.findByStepOrder(request.getCurrentApprovalLevel())
                .orElse(null); // Step might be null if it's an ad-hoc action or auto-approval

        Approval approvalLog = new Approval();
        approvalLog.setPurchaseRequest(request);
        approvalLog.setApprovalStep(currentStep); // Can be null
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
                .orElseThrow(() -> new IllegalStateException("Exchange rate not found for currency code: " + request.getCurrency().getCurrencyCode() + " on or before " + request.getCreatedAt().toLocalDate()));
        return request.getNetAmount().multiply(exchangeRate.getRate());
    }
}