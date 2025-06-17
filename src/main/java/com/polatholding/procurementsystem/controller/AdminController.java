package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.dto.AdminUserFormDto;
import com.polatholding.procurementsystem.service.AdminService;
import com.polatholding.procurementsystem.validation.OnCreate;
import com.polatholding.procurementsystem.validation.OnUpdate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('Admin')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", adminService.getAllUsers());
        return "admin-users";
    }

    @GetMapping("/users/new")
    public String showCreateUserForm(Model model) {
        model.addAttribute("userForm", new AdminUserFormDto());
        model.addAttribute("departments", adminService.getAllDepartments());
        model.addAttribute("allRoles", adminService.getAllRoles());
        model.addAttribute("isEditMode", false);
        return "admin-user-form";
    }

    @PostMapping("/users/save") // This is the CREATE user method
    public String saveNewUser(@Validated(OnCreate.class) @ModelAttribute("userForm") AdminUserFormDto userForm,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", adminService.getAllDepartments());
            model.addAttribute("allRoles", adminService.getAllRoles());
            model.addAttribute("isEditMode", false);
            return "admin-user-form";
        }
        try {
            adminService.createUser(userForm);
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully: " + userForm.getEmail());
        } catch (IllegalArgumentException e) {
            model.addAttribute("departments", adminService.getAllDepartments());
            model.addAttribute("allRoles", adminService.getAllRoles());
            model.addAttribute("isEditMode", false);
            model.addAttribute("pageErrorMessage", e.getMessage());
            return "admin-user-form";
        } catch (Exception e) {
            model.addAttribute("departments", adminService.getAllDepartments());
            model.addAttribute("allRoles", adminService.getAllRoles());
            model.addAttribute("isEditMode", false);
            model.addAttribute("pageErrorMessage", "An unexpected error occurred: " + e.getMessage());
            return "admin-user-form";
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Integer userId, Model model, RedirectAttributes redirectAttributes) {
        try {
            AdminUserFormDto userForm = adminService.getUserFormById(userId);
            model.addAttribute("userForm", userForm);
            model.addAttribute("departments", adminService.getAllDepartments());
            model.addAttribute("allRoles", adminService.getAllRoles());
            model.addAttribute("isEditMode", true);
            return "admin-user-form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found with ID: " + userId + ". " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/users/update") // This is the UPDATE user method
    public String updateUser(@Validated(OnUpdate.class) @ModelAttribute("userForm") AdminUserFormDto userForm,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (userForm.getUserId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "User ID is missing for update.");
            return "redirect:/admin/users";
        }

        // Explicitly check password length if provided during update
        // This is because the @Size on DTO is only for OnCreate group
        if (userForm.getPassword() != null && !userForm.getPassword().isEmpty() && userForm.getPassword().length() < 8) {
            // Add error to BindingResult to display on the form
            bindingResult.rejectValue("password", "Size.userForm.password", "Password must be at least 8 characters long if provided.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", adminService.getAllDepartments());
            model.addAttribute("allRoles", adminService.getAllRoles());
            model.addAttribute("isEditMode", true);
            return "admin-user-form";
        }

        try {
            adminService.updateUser(userForm);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully: " + userForm.getEmail());
        } catch (IllegalArgumentException e) {
            model.addAttribute("departments", adminService.getAllDepartments());
            model.addAttribute("allRoles", adminService.getAllRoles());
            model.addAttribute("isEditMode", true);
            model.addAttribute("pageErrorMessage", e.getMessage());
            return "admin-user-form";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("departments", adminService.getAllDepartments());
            model.addAttribute("allRoles", adminService.getAllRoles());
            model.addAttribute("isEditMode", true);
            String message = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : "Invalid data";
            model.addAttribute("pageErrorMessage", "Data integrity violation: " + message);
            return "admin-user-form";
        } catch (Exception e) {
            model.addAttribute("departments", adminService.getAllDepartments());
            model.addAttribute("allRoles", adminService.getAllRoles());
            model.addAttribute("isEditMode", true);
            String message = e.getMessage() != null ? e.getMessage() : "Please check the submitted data.";
            model.addAttribute("pageErrorMessage", "An unexpected error occurred: " + message);
            return "admin-user-form";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/toggle-status/{id}")
    public String toggleUserActiveStatus(@PathVariable("id") Integer userId, RedirectAttributes redirectAttributes) {
        try {
            adminService.toggleUserActiveStatus(userId);
            redirectAttributes.addFlashAttribute("successMessage", "User status toggled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to toggle user status: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}