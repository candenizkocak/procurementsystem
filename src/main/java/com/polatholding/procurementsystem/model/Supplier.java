package com.polatholding.procurementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SupplierID")
    private Integer supplierId;

    @Column(name = "SupplierName", nullable = false, length = 100)
    private String supplierName;

    @Column(name = "ContactPerson", length = 100)
    private String contactPerson;

    @Column(name = "Email", length = 100)
    private String email;

    @Column(name = "Phone", length = 30)
    private String phone;

    @Column(name = "Description", length = 200)
    private String description;

    @Column(name = "Status", nullable = false, length = 20)
    private String status;
}