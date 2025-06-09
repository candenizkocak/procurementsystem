package com.polatholding.procurementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Approvals")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ApprovalID")
    private Integer approvalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequestID", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApprovalStepID", nullable = true) // Changed from nullable=false
    private ApprovalStep approvalStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApproverUserID", nullable = false)
    private User approverUser;

    @Column(name = "ApprovalStatus", nullable = false, length = 30)
    private String approvalStatus; // e.g., "Approved", "Rejected"

    @Column(name = "ApprovalDate")
    private LocalDateTime approvalDate;

    @Column(name = "RejectReason", length = 200)
    private String rejectReason;
}