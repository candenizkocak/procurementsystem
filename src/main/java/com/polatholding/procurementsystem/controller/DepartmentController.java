package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.dto.DepartmentFormDto;
import com.polatholding.procurementsystem.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/departments")
@PreAuthorize("hasRole('Admin')")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public String listDepartments(Model model) {
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "admin-departments";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("departmentForm", new DepartmentFormDto());
        model.addAttribute("isEditMode", false);
        return "department-form";
    }

    @PostMapping("/save")
    public String saveDepartment(@Valid @ModelAttribute("departmentForm") DepartmentFormDto formDto,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEditMode", false);
            return "department-form";
        }
        departmentService.createDepartment(formDto);
        redirectAttributes.addFlashAttribute("successMessage", "Department created successfully.");
        return "redirect:/admin/departments";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("departmentForm", departmentService.getDepartmentFormById(id));
            model.addAttribute("isEditMode", true);
            return "department-form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Department not found with ID: " + id);
            return "redirect:/admin/departments";
        }
    }

    @PostMapping("/update/{id}")
    public String updateDepartment(@PathVariable("id") Integer id,
                                   @Valid @ModelAttribute("departmentForm") DepartmentFormDto formDto,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEditMode", true);
            return "department-form";
        }
        departmentService.updateDepartment(id, formDto);
        redirectAttributes.addFlashAttribute("successMessage", "Department updated successfully.");
        return "redirect:/admin/departments";
    }

    @PostMapping("/delete/{id}")
    public String deleteDepartment(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        boolean deleted = departmentService.deleteDepartment(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("successMessage", "Department deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Department has active employees and cannot be deleted.");
        }
        return "redirect:/admin/departments";
    }
}
