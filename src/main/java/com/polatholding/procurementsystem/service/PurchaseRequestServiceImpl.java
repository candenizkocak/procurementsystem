package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.*;
import com.polatholding.procurementsystem.exception.InsufficientBudgetException;
import com.polatholding.procurementsystem.model.*;
import com.polatholding.procurementsystem.repository.*;
import com.polatholding.procurementsystem.service.RequestHistoryService;
import com.polatholding.procurementsystem.service.FileService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;
    private final BudgetCodeRepository budgetCodeRepository;
    private final CurrencyRepository currencyRepository;
    private final SupplierRepository supplierRepository;
    private final UnitRepository unitRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final RequestHistoryService requestHistoryService;
    private final FileService fileService;

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
                                      FileService fileService) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.userRepository = userRepository;
        this.budgetCodeRepository = budgetCodeRepository;
        this.currencyRepository = currencyRepository;
        this.supplierRepository = supplierRepository;
        this.unitRepository = unitRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.requestHistoryService = requestHistoryService;
        this.fileService = fileService;
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

        // --- THIS IS THE FIX for Data Filtering Flaw ---
        // Instead of findAll(), we fetch only 'Active' suppliers for the dropdown.
        initDto.setSuppliers(supplierRepository.findByStatusOrderBySupplierNameAsc("Active"));
        // ---------------------------------------------

        initDto.setUnits(unitRepository.findAll());
        return initDto;
    }

    // ... all other methods remain unchanged. Included for completeness.

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

        if (!requestToUpdate.getCreatedByUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You are not authorized to update this request.");
        }
        if (!"Returned for Edit".equals(requestToUpdate.getStatus())) {
            throw new IllegalStateException("This request cannot be edited as it is not in 'Returned for Edit' status.");
        }

        requestToUpdate.setBudgetCode(budgetCodeRepository.findById(formDto.getBudgetCodeId()).orElseThrow());
        requestToUpdate.setCurrency(currencyRepository.findById(formDto.getCurrencyId()).orElseThrow());

        requestToUpdate.getItems().clear();
        purchaseRequestRepository.flush();

        BigDecimal totalNetAmount = BigDecimal.ZERO;
        for (RequestItemFormDto itemDto : formDto.getItems()) {
            if (itemDto.getItemName() == null || itemDto.getItemName().trim().isEmpty()) continue;
            PurchaseRequestItem newItem = new PurchaseRequestItem();
            newItem.setPurchaseRequest(requestToUpdate);
            newItem.setItemName(itemDto.getItemName());
            newItem.setQuantity(itemDto.getQuantity());
            newItem.setUnitPrice(itemDto.getUnitPrice());
            newItem.setDescription(itemDto.getDescription());
            newItem.setSupplier(supplierRepository.findById(itemDto.getSupplierId()).orElseThrow());
            newItem.setUnit(unitRepository.findById(itemDto.getUnitId()).orElseThrow());
            requestToUpdate.getItems().add(newItem);
            totalNetAmount = totalNetAmount.add(itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
        }
        requestToUpdate.setNetAmount(totalNetAmount);
        requestToUpdate.setGrossAmount(totalNetAmount.multiply(new BigDecimal("1.20")));

        requestToUpdate.setStatus("Pending");
        Set<String> userRoles = requestToUpdate.getCreatedByUser().getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());
        if (userRoles.contains(MANAGER_ROLE_NAME) || userRoles.contains(FINANCE_OFFICER_ROLE_NAME)) {
            requestToUpdate.setCurrentApprovalLevel(2);
        } else {
            requestToUpdate.setCurrentApprovalLevel(1);
        }
        requestToUpdate.setRejectReason(null);
        purchaseRequestRepository.save(requestToUpdate);
        fileService.uploadFiles(requestId, files, userEmail);
        requestHistoryService.logAction(requestId, userEmail, "Resubmitted", null);
    }

    @Override
    @Transactional
    public Integer saveNewRequest(PurchaseRequestFormDto formDto, String userEmail, List<MultipartFile> files) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        BudgetCode budgetCode = budgetCodeRepository.findById(formDto.getBudgetCodeId()).orElseThrow();

        PurchaseRequest newRequest = new PurchaseRequest();
        newRequest.setCreatedByUser(currentUser);
        newRequest.setDepartment(currentUser.getDepartment());
        newRequest.setBudgetCode(budgetCode);
        newRequest.setCurrency(currencyRepository.findById(formDto.getCurrencyId()).orElseThrow());
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
            newItem.setSupplier(supplierRepository.findById(itemDto.getSupplierId()).orElseThrow());
            newItem.setUnit(unitRepository.findById(itemDto.getUnitId()).orElseThrow());
            newRequest.getItems().add(newItem);
            BigDecimal itemTotalPrice = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalNetAmount = totalNetAmount.add(itemTotalPrice);
        }
        newRequest.setNetAmount(totalNetAmount);
        newRequest.setGrossAmount(totalNetAmount.multiply(new BigDecimal("1.20")));

        Set<String> userRoles = currentUser.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());
        boolean isAutoApproved = false;
        if (userRoles.contains(DIRECTOR_ROLE_NAME)) {
            newRequest.setStatus("Approved");
            newRequest.setCurrentApprovalLevel(99);
            isAutoApproved = true;
        } else if (userRoles.contains(PROCUREMENT_MANAGER_ROLE_NAME)) {
            BigDecimal valueInTRY = calculateRequestValueInTRY(newRequest);
            if (valueInTRY.compareTo(HIGH_VALUE_THRESHOLD) > 0) {
                newRequest.setStatus("Pending");
                newRequest.setCurrentApprovalLevel(3);
            } else {
                newRequest.setStatus("Approved");
                newRequest.setCurrentApprovalLevel(99);
                isAutoApproved = true;
            }
        } else if (userRoles.contains(MANAGER_ROLE_NAME) || userRoles.contains(FINANCE_OFFICER_ROLE_NAME)) {
            newRequest.setStatus("Pending");
            newRequest.setCurrentApprovalLevel(2);
        } else {
            newRequest.setStatus("Pending");
            newRequest.setCurrentApprovalLevel(1);
        }

        if (isAutoApproved) {
            consumeBudgetForAutoApprovedRequest(newRequest);
        }

        purchaseRequestRepository.save(newRequest);
        fileService.uploadFiles(newRequest.getRequestId(), files, userEmail);
        return newRequest.getRequestId();
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
        boolean isPrivileged = user.getRoles().stream().anyMatch(role -> Set.of("Manager", "ProcurementManager", "Director", "Admin", "Finance", "Auditor").contains(role.getRoleName()));
        List<PurchaseRequest> requests = isPrivileged
                ? purchaseRequestRepository.findAllWithDetails()
                : purchaseRequestRepository.findByCreatorIdWithDetails(user.getUserId());
        return requests.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestDto> getPendingApprovalsForUser(String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));
        List<PurchaseRequest> managerQueue = new ArrayList<>();
        if (currentUser.getRoles().stream().anyMatch(role -> MANAGER_ROLE_NAME.equals(role.getRoleName()))) {
            managerQueue = purchaseRequestRepository.findPendingDepartmentManagerApprovals(currentUser.getUserId());
        }
        Set<Integer> generalRoleIds = currentUser.getRoles().stream()
                .filter(role -> Set.of(PROCUREMENT_MANAGER_ROLE_NAME, DIRECTOR_ROLE_NAME).contains(role.getRoleName()))
                .map(Role::getRoleId)
                .collect(Collectors.toSet());
        List<PurchaseRequest> roleQueue = new ArrayList<>();
        if (!generalRoleIds.isEmpty()) {
            roleQueue = purchaseRequestRepository.findPendingApprovalsByRoleIds(generalRoleIds);
        }
        return Stream.concat(managerQueue.stream(), roleQueue.stream()).distinct().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public List<PurchaseRequestDto> getAllRequests() {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseRequestDetailDto getRequestDetailsById(Integer requestId) {
        PurchaseRequest request = purchaseRequestRepository.findByIdWithAllDetails(requestId)
                .orElseThrow(() -> new RuntimeException("Purchase Request not found with ID: " + requestId));
        return convertToDetailDto(request);
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
        dto.setGrossAmount(request.getGrossAmount());
        dto.setCurrencyCode(request.getCurrency().getCurrencyCode());
        dto.setItems(request.getItems());
        dto.setFiles(request.getFiles());
        return dto;
    }
    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestDto> searchUserRequests(String userEmail, String searchTerm) {
        // We get all requests found by the search term first.
        List<PurchaseRequest> allFoundRequests = purchaseRequestRepository.searchByItemFreetext(searchTerm);

        // Then, we filter them based on the user's permissions in Java.
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        boolean isPrivileged = user.getRoles().stream().anyMatch(role -> Set.of("Manager", "ProcurementManager", "Director", "Admin", "Finance", "Auditor").contains(role.getRoleName()));

        if (isPrivileged) {
            // Privileged users can see all search results.
            return allFoundRequests.stream().map(this::convertToDto).collect(Collectors.toList());
        } else {
            // Standard users only see results they created.
            return allFoundRequests.stream()
                    .filter(req -> req.getCreatedByUser().getUserId().equals(user.getUserId()))
                    .map(this::convertToDto)
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

