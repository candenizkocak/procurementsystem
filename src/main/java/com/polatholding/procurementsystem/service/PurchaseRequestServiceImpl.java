package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.*;
import com.polatholding.procurementsystem.exception.InsufficientBudgetException;
import com.polatholding.procurementsystem.model.*;
import com.polatholding.procurementsystem.repository.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseRequestServiceImpl.class);

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;
    private final BudgetCodeRepository budgetCodeRepository;
    private final CurrencyRepository currencyRepository;
    private final SupplierRepository supplierRepository;
    private final UnitRepository unitRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final RequestHistoryService requestHistoryService;
    private final FileService fileService;
    private final com.polatholding.procurementsystem.repository.DatabaseHelperRepository dbHelper;
    private final NotificationService notificationService; // Added

    private static final String DIRECTOR_ROLE_NAME = "Director";
    private static final String PROCUREMENT_MANAGER_ROLE_NAME = "ProcurementManager";
    private static final String MANAGER_ROLE_NAME = "Manager";
    private static final String FINANCE_OFFICER_ROLE_NAME = "Finance Officer";
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000000");


    public PurchaseRequestServiceImpl(PurchaseRequestRepository purchaseRequestRepository,
                                      UserRepository userRepository,
                                      BudgetCodeRepository budgetCodeRepository,
                                      CurrencyRepository currencyRepository,
                                      SupplierRepository supplierRepository,
                                      UnitRepository unitRepository,
                                      ExchangeRateRepository exchangeRateRepository,
                                      RequestHistoryService requestHistoryService,
                                      FileService fileService,
                                      com.polatholding.procurementsystem.repository.DatabaseHelperRepository dbHelper,
                                      NotificationService notificationService) { // Added notificationService
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.userRepository = userRepository;
        this.budgetCodeRepository = budgetCodeRepository;
        this.currencyRepository = currencyRepository;
        this.supplierRepository = supplierRepository;
        this.unitRepository = unitRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.requestHistoryService = requestHistoryService;
        this.fileService = fileService;
        this.dbHelper = dbHelper;
        this.notificationService = notificationService; // Added assignment
    }

    @Override
    @Transactional(readOnly = true)
    public NewRequestFormInitDto getNewRequestFormData(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        NewRequestFormInitDto initDto = new NewRequestFormInitDto();

        List<BudgetCode> filteredBudgetCodes = budgetCodeRepository.findByDepartmentAndIsActiveTrue(user.getDepartment());
        initDto.setBudgetCodes(filteredBudgetCodes);
        initDto.setCurrencies(currencyRepository.findAll());
        initDto.setSuppliers(supplierRepository.findByStatusOrderBySupplierNameAsc("Active"));
        initDto.setUnits(unitRepository.findAll());
        return initDto;
    }

    @Override
    @Transactional(readOnly = true)
    public String getUserFullName(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user.getFirstName() + " " + user.getLastName();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseRequestFormDto getRequestFormById(Integer requestId) {
        PurchaseRequest request = purchaseRequestRepository.findByIdWithAllDetails(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

        PurchaseRequestFormDto formDto = new PurchaseRequestFormDto();
        formDto.setBudgetCodeId(request.getBudgetCode().getBudgetCodeId());
        formDto.setCurrencyId(request.getCurrency().getCurrencyId());

        List<RequestItemFormDto> itemDtos = request.getItems().stream().map(item -> {
            RequestItemFormDto itemDto = new RequestItemFormDto();
            itemDto.setItemName(item.getItemName());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setDescription(item.getDescription());
            itemDto.setSupplierId(item.getSupplier().getSupplierId());
            itemDto.setUnitId(item.getUnit().getUnitId());
            return itemDto;
        }).collect(Collectors.toList());

        formDto.setItems(itemDtos);
        return formDto;
    }

    @Override
    @Transactional
    public void updateRequest(Integer requestId, PurchaseRequestFormDto formDto, String userEmail, List<MultipartFile> files) {
        PurchaseRequest requestToUpdate = purchaseRequestRepository.findByIdWithAllDetails(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        if (!requestToUpdate.getCreatedByUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("You are not authorized to update this request.");
        }
        if (!"Returned for Edit".equals(requestToUpdate.getStatus())) {
            throw new IllegalStateException("This request cannot be edited as it is not in 'Returned for Edit' status.");
        }

        requestToUpdate.setCreatedByUser(currentUser); // Ensure the creator is correctly set if it wasn't fetched eagerly before
        requestToUpdate.setDepartment(currentUser.getDepartment()); // Reset department from current user

        requestToUpdate.setBudgetCode(budgetCodeRepository.findById(formDto.getBudgetCodeId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Budget Code ID")));
        requestToUpdate.setCurrency(currencyRepository.findById(formDto.getCurrencyId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Currency ID")));

        // Clear existing items and re-add from form
        requestToUpdate.getItems().clear();
        purchaseRequestRepository.flush(); // Persist the removal of items

        BigDecimal totalNetAmount = BigDecimal.ZERO;
        for (RequestItemFormDto itemDto : formDto.getItems()) {
            if (itemDto.getItemName() == null || itemDto.getItemName().trim().isEmpty()) continue;
            PurchaseRequestItem newItem = new PurchaseRequestItem();
            newItem.setPurchaseRequest(requestToUpdate);
            newItem.setItemName(itemDto.getItemName());
            newItem.setQuantity(itemDto.getQuantity());
            newItem.setUnitPrice(itemDto.getUnitPrice());
            newItem.setDescription(itemDto.getDescription());
            newItem.setSupplier(supplierRepository.findById(itemDto.getSupplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Supplier ID for item: " + itemDto.getItemName())));
            newItem.setUnit(unitRepository.findById(itemDto.getUnitId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Unit ID for item: " + itemDto.getItemName())));
            requestToUpdate.getItems().add(newItem);
            totalNetAmount = totalNetAmount.add(itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
        }
        requestToUpdate.setNetAmount(totalNetAmount);
        requestToUpdate.setGrossAmount(dbHelper.calculateGrossAmount(totalNetAmount)); // Use UDF

        requestToUpdate.setStatus("Pending"); // Resubmitted requests go back to Pending
        Set<String> userRoles = currentUser.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());

        // Determine initial approval level upon resubmission
        // This logic mirrors the one in saveNewRequest for consistency
        if (userRoles.contains(DIRECTOR_ROLE_NAME) || userRoles.contains(PROCUREMENT_MANAGER_ROLE_NAME)) {
            // For Directors/Procurement Managers, if they edit their own request, it still might need Director approval if high value
            BigDecimal valueInTRY = calculateRequestValueInTRY(requestToUpdate);
            if (valueInTRY.compareTo(HIGH_VALUE_THRESHOLD) > 0 && userRoles.contains(PROCUREMENT_MANAGER_ROLE_NAME) && !userRoles.contains(DIRECTOR_ROLE_NAME)) {
                requestToUpdate.setCurrentApprovalLevel(3); // Needs Director
            } else {
                // If Director is editing, or Proc Manager is editing a low-value request, it's considered auto-approved by them
                // This scenario for 'editing' is a bit tricky. Usually, edits re-trigger the full flow.
                // For simplicity, let's assume if a high-level user edits and resubmits, it follows their initial submission logic.
                // Here, we can simplify: if edited by ProcMan/Director, it may not need *their own* first level of approval again.
                // It should go to the *next logical step* or be considered approved if they are the highest step.
                // Let's make it go to Level 2 (ProcurementManager) if submitted by a regular Manager or Employee.
                // If edited by Manager/Finance -> Level 2
                // If edited by Employee -> Level 1
                // This logic needs to be robust for who is editing.
                // For now, let's assume it resets to the standard flow based on *editor's* role, not original submitter's level
                if (userRoles.contains(MANAGER_ROLE_NAME) || userRoles.contains(FINANCE_OFFICER_ROLE_NAME)) {
                    requestToUpdate.setCurrentApprovalLevel(2); // Goes to Procurement Manager
                } else { // Employee
                    requestToUpdate.setCurrentApprovalLevel(1); // Goes to Department Manager
                }
            }
        } else if (userRoles.contains(MANAGER_ROLE_NAME) || userRoles.contains(FINANCE_OFFICER_ROLE_NAME)) {
            requestToUpdate.setCurrentApprovalLevel(2); // Goes to Procurement Manager
        } else { // Employee
            requestToUpdate.setCurrentApprovalLevel(1); // Goes to Department Manager
        }


        requestToUpdate.setRejectReason(null); // Clear previous rejection/return comments
        // CreatedAt is not updated.
        PurchaseRequest updatedRequest = purchaseRequestRepository.save(requestToUpdate);

        if (!files.isEmpty() && files.stream().anyMatch(f -> !f.isEmpty())) {
            fileService.uploadFiles(updatedRequest.getRequestId(), files, userEmail);
        }

        requestHistoryService.logAction(updatedRequest.getRequestId(), userEmail, "Resubmitted", "Request updated and resubmitted for approval.");

        if ("Pending".equalsIgnoreCase(updatedRequest.getStatus())) {
            notificationService.notifyRequestSubmission(updatedRequest);
        }
    }

    @Override
    @Transactional
    public Integer saveNewRequest(PurchaseRequestFormDto formDto, String userEmail, List<MultipartFile> files) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        BudgetCode budgetCode = budgetCodeRepository.findById(formDto.getBudgetCodeId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Budget Code ID"));

        PurchaseRequest newRequest = new PurchaseRequest();
        newRequest.setCreatedByUser(currentUser);
        newRequest.setDepartment(currentUser.getDepartment());
        newRequest.setBudgetCode(budgetCode);
        newRequest.setCurrency(currencyRepository.findById(formDto.getCurrencyId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Currency ID")));
        newRequest.setCreatedAt(LocalDateTime.now());

        newRequest.setItems(new ArrayList<>());
        BigDecimal totalNetAmount = BigDecimal.ZERO;
        for (RequestItemFormDto itemDto : formDto.getItems()) {
            if (itemDto.getItemName() == null || itemDto.getItemName().trim().isEmpty()) continue;
            PurchaseRequestItem newItem = new PurchaseRequestItem();
            newItem.setPurchaseRequest(newRequest);
            newItem.setItemName(itemDto.getItemName());
            newItem.setQuantity(itemDto.getQuantity());
            newItem.setUnitPrice(itemDto.getUnitPrice());
            newItem.setDescription(itemDto.getDescription());
            newItem.setSupplier(supplierRepository.findById(itemDto.getSupplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Supplier ID for item: " + itemDto.getItemName())));
            newItem.setUnit(unitRepository.findById(itemDto.getUnitId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Unit ID for item: " + itemDto.getItemName())));
            newRequest.getItems().add(newItem);
            BigDecimal itemTotalPrice = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalNetAmount = totalNetAmount.add(itemTotalPrice);
        }
        newRequest.setNetAmount(totalNetAmount);
        newRequest.setGrossAmount(dbHelper.calculateGrossAmount(totalNetAmount)); // Use UDF

        Set<String> userRoles = currentUser.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());
        boolean isAutoApproved = false;

        if (userRoles.contains(DIRECTOR_ROLE_NAME)) {
            newRequest.setStatus("Approved");
            newRequest.setCurrentApprovalLevel(99); // Special level for auto-approved
            isAutoApproved = true;
        } else if (userRoles.contains(PROCUREMENT_MANAGER_ROLE_NAME)) {
            BigDecimal valueInTRY = calculateRequestValueInTRY(newRequest);
            if (valueInTRY.compareTo(HIGH_VALUE_THRESHOLD) > 0) {
                newRequest.setStatus("Pending");
                newRequest.setCurrentApprovalLevel(3); // Needs Director approval
            } else {
                newRequest.setStatus("Approved");
                newRequest.setCurrentApprovalLevel(99); // Special level for auto-approved
                isAutoApproved = true;
            }
        } else if (userRoles.contains(MANAGER_ROLE_NAME) || userRoles.contains(FINANCE_OFFICER_ROLE_NAME)) {
            newRequest.setStatus("Pending");
            newRequest.setCurrentApprovalLevel(2); // Needs Procurement Manager approval
        } else { // Employee
            newRequest.setStatus("Pending");
            newRequest.setCurrentApprovalLevel(1); // Needs Department Manager approval
        }

        if (isAutoApproved) {
            consumeBudgetForAutoApprovedRequest(newRequest);
        }

        PurchaseRequest savedRequest = purchaseRequestRepository.save(newRequest);

        if (!files.isEmpty() && files.stream().anyMatch(f -> !f.isEmpty())) {
            fileService.uploadFiles(savedRequest.getRequestId(), files, userEmail);
        }

        requestHistoryService.logAction(savedRequest.getRequestId(), userEmail, "Created", "New request submitted.");

        if ("Pending".equalsIgnoreCase(savedRequest.getStatus())) {
            notificationService.notifyRequestSubmission(savedRequest);
        } else if ("Approved".equalsIgnoreCase(savedRequest.getStatus())) {
            // If auto-approved by a high-level user submitting it
            notificationService.createNotification(
                    savedRequest.getCreatedByUser(),
                    savedRequest,
                    NotificationServiceImpl.TYPE_REQUEST_APPROVED,
                    "Your request #" + savedRequest.getRequestId() + " was auto-approved upon submission.",
                    "/requests/" + savedRequest.getRequestId()
            );
        }
        return savedRequest.getRequestId();
    }

    private void consumeBudgetForAutoApprovedRequest(PurchaseRequest request) {
        BudgetCode budgetCode = request.getBudgetCode();
        BigDecimal requestAmountInTRY = calculateRequestValueInTRY(request);
        if (budgetCode.getBudgetAmount().compareTo(requestAmountInTRY) < 0) {
            throw new InsufficientBudgetException(
                    String.format("Request creation failed: Insufficient funds in budget code '%s'. Remaining: %.2f, Required: %.2f",
                            budgetCode.getCode(),
                            budgetCode.getBudgetAmount(),
                            requestAmountInTRY)
            );
        }
        BigDecimal newBudgetAmount = budgetCode.getBudgetAmount().subtract(requestAmountInTRY);
        budgetCode.setBudgetAmount(newBudgetAmount);
        budgetCodeRepository.save(budgetCode); // Save updated budget
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

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestDto> getRequestsForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        boolean isPrivileged = user.getRoles().stream().anyMatch(role ->
                Set.of(MANAGER_ROLE_NAME, PROCUREMENT_MANAGER_ROLE_NAME, DIRECTOR_ROLE_NAME, AdminServiceImpl.ADMIN_ROLE_NAME, FINANCE_OFFICER_ROLE_NAME, AdminServiceImpl.AUDITOR_ROLE_NAME)
                        .contains(role.getRoleName()));

        List<PurchaseRequest> requests;
        if (isPrivileged) {
            requests = purchaseRequestRepository.findAllWithDetails();
        } else {
            requests = purchaseRequestRepository.findByCreatorIdWithDetails(user.getUserId());
        }
        return requests.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestDto> getPendingApprovalsForUser(String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        // Fetch requests needing department manager approval if user is a manager
        List<PurchaseRequest> managerApprovalQueue = new ArrayList<>();
        if (currentUser.getRoles().stream().anyMatch(role -> MANAGER_ROLE_NAME.equals(role.getRoleName()))) {
            // A user is a manager of a department if their UserID is the ManagerUserID in the Departments table.
            // We need to find which departments this user manages.
            List<Department> managedDepartments = currentUser.getManagedDepartments(); // Assuming User entity has a @OneToMany to Departments where they are manager
            if (managedDepartments != null && !managedDepartments.isEmpty()) {
                for (Department dept : managedDepartments) {
                    managerApprovalQueue.addAll(purchaseRequestRepository.findPendingDepartmentManagerApprovals(currentUser.getUserId(), dept.getDepartmentId()));
                }
            }
        }

        // Fetch requests needing approval based on roles (ProcurementManager, Director)
        Set<Integer> generalRoleIds = currentUser.getRoles().stream()
                .filter(role -> Set.of(PROCUREMENT_MANAGER_ROLE_NAME, DIRECTOR_ROLE_NAME).contains(role.getRoleName()))
                .map(Role::getRoleId)
                .collect(Collectors.toSet());

        List<PurchaseRequest> roleBasedApprovalQueue = new ArrayList<>();
        if (!generalRoleIds.isEmpty()) {
            roleBasedApprovalQueue = purchaseRequestRepository.findPendingApprovalsByRoleIds(generalRoleIds);
        }

        // Combine and convert to DTOs
        Stream<PurchaseRequestDto> managerDtoStream = managerApprovalQueue.stream().map(this::convertToDto);
        Stream<PurchaseRequestDto> roleDtoStream = roleBasedApprovalQueue.stream().map(this::convertToDto);

        return Stream.concat(managerDtoStream, roleDtoStream)
                .distinct() // Avoid duplicates if a request falls into multiple categories for the same user (unlikely with current logic)
                .collect(Collectors.toList());
    }

    // You'll need to adjust PurchaseRequestRepository for findPendingDepartmentManagerApprovals to take departmentId
    // And add getManagedDepartments to User model.
    // Example for User model:
    // @OneToMany(mappedBy = "managerUser")
    // private List<Department> managedDepartments;
    // Example for PurchaseRequestRepository:
    // @Query("SELECT pr FROM PurchaseRequest pr JOIN FETCH pr.createdByUser JOIN FETCH pr.department d JOIN FETCH pr.currency " +
    //        "WHERE pr.status = 'Pending' AND pr.currentApprovalLevel = 1 AND d.departmentId = :departmentId AND d.managerUser.userId = :managerId")
    // List<PurchaseRequest> findPendingDepartmentManagerApprovals(@Param("managerId") Integer managerId, @Param("departmentId") Integer departmentId);


    @Override
    public List<PurchaseRequestDto> getAllRequests() {
        // This method was empty, providing a basic implementation.
        // It might need more specific logic based on actual requirements (e.g., admin view)
        return purchaseRequestRepository.findAllWithDetails().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseRequestDetailDto getRequestDetailsById(Integer requestId) {
        PurchaseRequest request = purchaseRequestRepository.findByIdWithAllDetails(requestId)
                .orElseThrow(() -> new RuntimeException("Purchase Request not found with ID: " + requestId));

        PurchaseRequestDetailDto dto = convertToDetailDto(request);
        dto.setGrossAmount(dbHelper.calculateGrossAmount(dto.getNetAmount()));
        dto.setDaysSinceCreated(dbHelper.getDaysSinceRequest(requestId));
        return dto;
    }

    private PurchaseRequestDto convertToDto(PurchaseRequest request) {
        PurchaseRequestDto dto = new PurchaseRequestDto();
        dto.setRequestId(request.getRequestId());
        dto.setCreatorFullName(request.getCreatedByUser().getFirstName() + " " + request.getCreatedByUser().getLastName());
        dto.setDepartmentName(request.getDepartment().getDepartmentName());
        dto.setStatus(request.getStatus());
        dto.setNetAmount(request.getNetAmount());
        dto.setCurrencyCode(request.getCurrency().getCurrencyCode());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setRejectReason(request.getRejectReason());
        dto.setCreatorId(request.getCreatedByUser().getUserId());
        return dto;
    }

    private PurchaseRequestDetailDto convertToDetailDto(PurchaseRequest request) {
        PurchaseRequestDetailDto dto = new PurchaseRequestDetailDto();
        dto.setRequestId(request.getRequestId());
        dto.setCreatorFullName(request.getCreatedByUser().getFirstName() + " " + request.getCreatedByUser().getLastName());
        dto.setDepartmentName(request.getDepartment().getDepartmentName());
        dto.setStatus(request.getStatus());
        dto.setBudgetCode(request.getBudgetCode().getCode());
        dto.setRejectReason(request.getRejectReason());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setNetAmount(request.getNetAmount());
        dto.setGrossAmount(request.getGrossAmount()); // Use value from entity, as UDF is for calculation mostly
        dto.setCurrencyCode(request.getCurrency().getCurrencyCode());
        dto.setItems(new ArrayList<>(request.getItems())); // Ensure modifiable list if needed downstream, or defensive copy
        dto.setFiles(new ArrayList<>(request.getFiles())); // Same for files
        return dto;
    }

    private PurchaseRequestDto mapSummaryToDto(RequestSummaryViewDto summary) {
        PurchaseRequestDto dto = new PurchaseRequestDto();
        dto.setRequestId(summary.getRequestId());
        dto.setCreatorFullName(summary.getCreator());
        dto.setDepartmentName(summary.getDepartmentName());
        dto.setStatus(summary.getStatus());
        dto.setNetAmount(summary.getNetAmount());
        dto.setCurrencyCode(summary.getCurrencyCode());
        dto.setCreatedAt(summary.getCreatedAt());
        dto.setRejectReason(summary.getRejectReason());
        // Creator ID is not in RequestSummaryViewDto, might need to adjust or fetch if strictly needed here
        return dto;
    }
    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestDto> searchUserRequests(String userEmail, String searchTerm) {
        // Using native query with FREETEXT which is defined in PurchaseRequestRepository
        List<PurchaseRequest> freetextResults = purchaseRequestRepository.searchByItemFreetext(searchTerm);

        // Convert to DTOs
        List<PurchaseRequestDto> allFoundRequests = freetextResults.stream()
                .map(this::convertToDto) // Assuming convertToDto fetches necessary details or they are part of freetextResults
                .collect(Collectors.toList());


        // Then, filter them based on the user's permissions in Java.
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        boolean isPrivileged = user.getRoles().stream().anyMatch(role ->
                Set.of(MANAGER_ROLE_NAME, PROCUREMENT_MANAGER_ROLE_NAME, DIRECTOR_ROLE_NAME, AdminServiceImpl.ADMIN_ROLE_NAME, FINANCE_OFFICER_ROLE_NAME, AdminServiceImpl.AUDITOR_ROLE_NAME)
                        .contains(role.getRoleName()));

        if (isPrivileged) {
            return allFoundRequests;
        } else {
            // Filter results to only those created by the current user
            final Integer currentUserId = user.getUserId();
            return allFoundRequests.stream()
                    .filter(reqDto -> reqDto.getCreatorId() != null && reqDto.getCreatorId().equals(currentUserId))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestDto> getRequestsByBudget(Integer budgetCodeId) {
        return purchaseRequestRepository.findByBudget(budgetCodeId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}