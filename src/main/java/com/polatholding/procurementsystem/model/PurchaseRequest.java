package com.polatholding.procurementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PurchaseRequests")
public class PurchaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RequestID")
    private Integer requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedByUserID", nullable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DepartmentID", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BudgetCodeID", nullable = false)
    private BudgetCode budgetCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CurrencyID", nullable = false)
    private Currency currency;

    @Column(name = "NetAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "GrossAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "Status", nullable = false, length = 30)
    private String status;

    @Column(name = "CurrentApprovalLevel", nullable = false)
    private int currentApprovalLevel;

    @Column(name = "RejectReason", length = 200)
    private String rejectReason;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // This establishes the link to the request items
    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PurchaseRequestItem> items;
}