package com.polatholding.procurementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PurchaseRequestItems")
public class PurchaseRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ItemID")
    private Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequestID", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SupplierID", nullable = false)
    private Supplier supplier;

    @Column(name = "ItemName", nullable = false, length = 100)
    private String itemName;

    @Column(name = "Quantity", nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UnitID", nullable = false)
    private Unit unit;

    @Column(name = "UnitPrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    // Let JPA know this column is calculated by the DB and should not be inserted/updated
    @Column(name = "TotalPrice", precision = 18, scale = 2, insertable = false, updatable = false)
    private BigDecimal totalPrice;

    @Column(name = "Description", length = 200)
    private String description;
}