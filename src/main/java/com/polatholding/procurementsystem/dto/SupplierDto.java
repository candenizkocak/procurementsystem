package com.polatholding.procurementsystem.dto;

import lombok.Data;

@Data
public class SupplierDto {
    private Integer supplierId;
    private String supplierName;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String status;
}