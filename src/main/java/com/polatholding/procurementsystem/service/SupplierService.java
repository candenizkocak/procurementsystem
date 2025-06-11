package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.SupplierDto;
import com.polatholding.procurementsystem.dto.SupplierFormDto;
import java.util.List;

public interface SupplierService {

    List<SupplierDto> getAllSuppliers();

    List<SupplierDto> getPendingSuppliers();

    void createNewSupplier(SupplierFormDto supplierFormDto);

    void approveSupplier(Integer supplierId);

    void rejectSupplier(Integer supplierId);

    List<SupplierDto> searchSuppliers(String searchTerm);

    SupplierFormDto getSupplierFormById(Integer supplierId);

    void updateSupplier(Integer supplierId, SupplierFormDto formDto);
}