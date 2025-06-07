package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.*;
import com.polatholding.procurementsystem.model.*;
import com.polatholding.procurementsystem.repository.*;
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
    private static final String MANAGER_ROLE_NAME = "Manager";
    private static final String DIRECTOR_ROLE_NAME = "Director"; // <-- Add Director role name constant

    public PurchaseRequestServiceImpl(PurchaseRequestRepository purchaseRequestRepository, UserRepository userRepository, BudgetCodeRepository budgetCodeRepository, CurrencyRepository currencyRepository, SupplierRepository supplierRepository, UnitRepository unitRepository) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.userRepository = userRepository;
        this.budgetCodeRepository = budgetCodeRepository;
        this.currencyRepository = currencyRepository;
        this.supplierRepository = supplierRepository;
        this.unitRepository = unitRepository;
    }

    @Override
    @Transactional
    public void saveNewRequest(PurchaseRequestFormDto formDto, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));
        BudgetCode budgetCode = budgetCodeRepository.findById(formDto.getBudgetCodeId()).orElseThrow();
        Currency currency = currencyRepository.findById(formDto.getCurrencyId()).orElseThrow();

        PurchaseRequest newRequest = new PurchaseRequest();
        newRequest.setCreatedByUser(currentUser);
        newRequest.setDepartment(currentUser.getDepartment());
        newRequest.setBudgetCode(budgetCode);
        newRequest.setCurrency(currency);
        newRequest.setCreatedAt(LocalDateTime.now());

        // *** FINAL LOGIC FOR STARTING LEVEL AND STATUS ***
        boolean creatorIsDirector = currentUser.getRoles().stream()
                .anyMatch(role -> DIRECTOR_ROLE_NAME.equals(role.getRoleName()));

        boolean creatorIsManager = currentUser.getRoles().stream()
                .anyMatch(role -> MANAGER_ROLE_NAME.equals(role.getRoleName()));

        if (creatorIsDirector) {
            // Directors' requests are instantly approved.
            newRequest.setStatus("Approved");
            newRequest.setCurrentApprovalLevel(99); // Use a special level for approved requests
        } else if (creatorIsManager) {
            // Managers' requests start at the Procurement level.
            newRequest.setStatus("Pending");
            newRequest.setCurrentApprovalLevel(2);
        } else {
            // Employees' requests start at the Manager level.
            newRequest.setStatus("Pending");
            newRequest.setCurrentApprovalLevel(1);
        }
        // *******************************************************

        newRequest.setItems(new ArrayList<>());
        BigDecimal totalNetAmount = BigDecimal.ZERO;
        for (RequestItemFormDto itemDto : formDto.getItems()) {
            if (itemDto.getItemName() == null || itemDto.getItemName().isEmpty()) continue;
            Supplier supplier = supplierRepository.findById(itemDto.getSupplierId()).orElseThrow();
            Unit unit = unitRepository.findById(itemDto.getUnitId()).orElseThrow();
            PurchaseRequestItem newItem = new PurchaseRequestItem();
            newItem.setPurchaseRequest(newRequest);
            newItem.setItemName(itemDto.getItemName());
            newItem.setQuantity(itemDto.getQuantity());
            newItem.setUnitPrice(itemDto.getUnitPrice());
            newItem.setDescription(itemDto.getDescription());
            newItem.setSupplier(supplier);
            newItem.setUnit(unit);
            newRequest.getItems().add(newItem);
            BigDecimal itemTotalPrice = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalNetAmount = totalNetAmount.add(itemTotalPrice);
        }
        newRequest.setNetAmount(totalNetAmount);
        newRequest.setGrossAmount(totalNetAmount.multiply(new BigDecimal("1.20")));
        purchaseRequestRepository.save(newRequest);
    }

    // --- ALL OTHER METHODS BELOW THIS LINE ARE UNCHANGED ---

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestDto> getRequestsForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        boolean isPrivileged = user.getRoles().stream().anyMatch(role -> Set.of("Manager", "ProcurementManager", "Director", "Admin", "Finance").contains(role.getRoleName()));
        List<PurchaseRequest> requests = isPrivileged ? purchaseRequestRepository.findAllWithDetails() : purchaseRequestRepository.findByCreatorIdWithDetails(user.getUserId());
        return requests.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestDto> getPendingApprovalsForUser(String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));
        List<PurchaseRequest> managerQueue = purchaseRequestRepository.findPendingDepartmentManagerApprovals(currentUser.getUserId());
        Set<Integer> generalRoleIds = currentUser.getRoles().stream()
                .filter(role -> Set.of("ProcurementManager", "Director").contains(role.getRoleName()))
                .map(Role::getRoleId)
                .collect(Collectors.toSet());
        List<PurchaseRequest> roleQueue = new ArrayList<>();
        if (!generalRoleIds.isEmpty()) {
            roleQueue = purchaseRequestRepository.findPendingApprovalsByRoleIds(generalRoleIds);
        }
        return Stream.concat(managerQueue.stream(), roleQueue.stream()).distinct().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public List<PurchaseRequestDto> getAllRequests() { return List.of(); }
    @Override
    public NewRequestFormInitDto getNewRequestFormData() {
        NewRequestFormInitDto initDto = new NewRequestFormInitDto();
        initDto.setBudgetCodes(budgetCodeRepository.findAll());
        initDto.setCurrencies(currencyRepository.findAll());
        initDto.setSuppliers(supplierRepository.findAll());
        initDto.setUnits(unitRepository.findAll());
        return initDto;
    }
    @Override
    public PurchaseRequestDetailDto getRequestDetailsById(Integer requestId) {
        PurchaseRequest request = purchaseRequestRepository.findById(requestId).orElseThrow();
        if (!request.getItems().isEmpty()) {
            request.getItems().get(0).getSupplier().getSupplierName();
        }
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
        return dto;
    }
}