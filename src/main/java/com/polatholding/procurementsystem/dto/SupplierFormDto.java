package com.polatholding.procurementsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierFormDto {
    private Integer supplierId;
    @NotEmpty(message = "Supplier name cannot be empty.")
    @Size(max = 100, message = "Supplier name cannot exceed 100 characters.")
    private String supplierName;

    @Size(max = 100, message = "Contact person name cannot exceed 100 characters.")
    private String contactPerson;

    @Email(message = "Please provide a valid email address.")
    @Size(max = 100)
    private String email;

    @Pattern(regexp = "^[0-9\\s\\+\\-\\(\\)]*$", message = "Phone number can only contain digits and symbols like +, -, ()")
    @Size(max = 30, message = "Phone number cannot exceed 30 characters.")
    private String phone;

    @Size(max = 200, message = "Address cannot exceed 200 characters.")
    private String address;
}