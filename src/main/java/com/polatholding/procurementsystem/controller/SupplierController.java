package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.dto.SupplierDto;
import com.polatholding.procurementsystem.dto.SupplierFormDto;
import com.polatholding.procurementsystem.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/suppliers")
// --- THIS IS THE FIX for the Security Flaw ---
// Only users with the 'ProcurementManager' role can now access these endpoints.
@PreAuthorize("hasRole('ProcurementManager')")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public String showSupplierList(Model model) {
        List<SupplierDto> suppliers = supplierService.getAllSuppliers();
        model.addAttribute("suppliers", suppliers);
        return "suppliers";
    }

    @GetMapping("/new")
    public String showNewSupplierForm(Model model) {
        model.addAttribute("supplierFormDto", new SupplierFormDto());
        return "supplier-form";
    }

    @PostMapping("/save")
    public String saveNewSupplier(@Valid @ModelAttribute("supplierFormDto") SupplierFormDto formDto,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "supplier-form";
        }
        supplierService.createNewSupplier(formDto);
        redirectAttributes.addFlashAttribute("successMessage", "New supplier created and sent for approval.");
        return "redirect:/suppliers";
    }

    @PostMapping("/approve/{id}")
    public String approveSupplier(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        supplierService.approveSupplier(id);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier approved and is now active.");
        return "redirect:/suppliers";
    }

    @PostMapping("/reject/{id}")
    public String rejectSupplier(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        supplierService.rejectSupplier(id);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier has been rejected.");
        return "redirect:/suppliers";
    }
}