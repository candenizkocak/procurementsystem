package com.polatholding.procurementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "BudgetCodes")
public class BudgetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BudgetCodeID")
    private Integer budgetCodeId;

    @Column(name = "Code", nullable = false, length = 30)
    private String code;

    @Column(name = "Description", length = 200)
    private String description;

    @Column(name = "Year", nullable = false)
    private int year;

    @Column(name = "BudgetAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "IsActive", nullable = false)
    private boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DepartmentID", nullable = false)
    private Department department;
}