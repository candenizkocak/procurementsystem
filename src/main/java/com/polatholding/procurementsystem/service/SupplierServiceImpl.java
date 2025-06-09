package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.SupplierDto;
import com.polatholding.procurementsystem.dto.SupplierFormDto;
import com.polatholding.procurementsystem.model.Supplier;
import com.polatholding.procurementsystem.repository.SupplierRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    @Transactional
    public void createNewSupplier(SupplierFormDto formDto) {
        Supplier newSupplier = new Supplier();
        BeanUtils.copyProperties(formDto, newSupplier);
        newSupplier.setStatus("Pending");
        supplierRepository.save(newSupplier);
    }

    @Override
    @Transactional
    public void approveSupplier(Integer supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found: " + supplierId));
        supplier.setStatus("Active");
        supplierRepository.save(supplier);
    }

    @Override
    @Transactional
    public void rejectSupplier(Integer supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found: " + supplierId));
        supplier.setStatus("Rejected");
        supplierRepository.save(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierDto> getPendingSuppliers() {
        return supplierRepository.findByStatus("Pending")
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierDto> getAllSuppliers() {
        // THE FIX: Calling the renamed repository method.
        List<Supplier> activeSuppliers = supplierRepository.findByStatusOrderBySupplierNameAsc("Active");
        List<Supplier> pendingSuppliers = supplierRepository.findByStatus("Pending");

        return Stream.concat(pendingSuppliers.stream(), activeSuppliers.stream())
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private SupplierDto convertToDto(Supplier supplier) {
        SupplierDto dto = new SupplierDto();
        BeanUtils.copyProperties(supplier, dto);
        return dto;
    }
}