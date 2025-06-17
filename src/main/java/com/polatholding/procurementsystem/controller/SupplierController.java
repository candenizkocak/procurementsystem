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
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    /**
     * THE FIX: There is now only ONE method mapped to GET /suppliers.
     * The @RequestParam(required = false) handles both cases:
     * - When the URL is /suppliers, 'query' is null.
     * - When the URL is /suppliers?q=search, 'query' has a value.
     */
    @GetMapping
    public String showSupplierList(@RequestParam(name = "q", required = false) String query, Model model) {
        List<SupplierDto> suppliers;
        if (query != null && !query.trim().isEmpty()) {
            suppliers = supplierService.searchSuppliers(query);
            model.addAttribute("searchTerm", query);
        } else {
            suppliers = supplierService.getAllSuppliers();
        }
        model.addAttribute("suppliers", suppliers);
        return "suppliers";
    }

    @GetMapping("/new")
    @PreAuthorize("@securityHelper.isProcurementStaff(authentication)")
    public String showNewSupplierForm(Model model) {
        model.addAttribute("supplierFormDto", new SupplierFormDto());
        model.addAttribute("isEditMode", false);
        return "supplier-form";
    }

    @PostMapping("/save")
    @PreAuthorize("@securityHelper.isProcurementStaff(authentication)")
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

    @GetMapping("/{id}/edit")
    @PreAuthorize("@securityHelper.isProcurementStaff(authentication)")
    public String showEditSupplierForm(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("supplierFormDto", supplierService.getSupplierFormById(id));
        model.addAttribute("isEditMode", true);
        return "supplier-form";
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("@securityHelper.isProcurementStaff(authentication)")
    public String updateSupplier(@PathVariable("id") Integer id,
                                 @Valid @ModelAttribute("supplierFormDto") SupplierFormDto formDto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEditMode", true);
            return "supplier-form";
        }
        supplierService.updateSupplier(id, formDto);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier updated successfully.");
        return "redirect:/suppliers";
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("@securityHelper.isProcurementStaff(authentication)")
    public String approveSupplier(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        supplierService.approveSupplier(id);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier approved and is now active.");
        return "redirect:/suppliers";
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("@securityHelper.isProcurementStaff(authentication)")
    public String rejectSupplier(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        supplierService.rejectSupplier(id);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier has been rejected.");
        return "redirect:/suppliers";
    }

    @PostMapping("/toggle-status/{id}")
    @PreAuthorize("@securityHelper.isProcurementStaff(authentication)")
    public String toggleSupplierStatus(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        supplierService.toggleSupplierStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier status has been updated.");
        return "redirect:/suppliers";
    }
}