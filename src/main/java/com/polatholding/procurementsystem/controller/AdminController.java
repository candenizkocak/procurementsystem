package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.dto.AdminUserFormDto;
import com.polatholding.procurementsystem.dto.SimpleDepartmentDto; // NEW
import com.polatholding.procurementsystem.dto.SimpleRoleDto;       // NEW
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

import java.util.stream.Collectors; // NEW

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

    private void addUserFormCommonAttributes(Model model) {
        model.addAttribute("jsDepartments", adminService.getAllDepartments().stream()
                .map(dept -> new SimpleDepartmentDto(dept.getDepartmentId(), dept.getDepartmentName()))
                .collect(Collectors.toList()));
        model.addAttribute("jsRoles", adminService.getAllRoles().stream()
                .map(role -> new SimpleRoleDto(role.getRoleId(), role.getRoleName()))
                .collect(Collectors.toList()));
        // Keep original for form population if needed, but JS uses jsRoles/jsDepartments
        model.addAttribute("departments", adminService.getAllDepartments());
        model.addAttribute("allRoles", adminService.getAllRoles());
    }


    @GetMapping("/users/new")
    public String showCreateUserForm(Model model) {
        model.addAttribute("userForm", new AdminUserFormDto());
        addUserFormCommonAttributes(model); // Use helper
        model.addAttribute("isEditMode", false);
        return "admin-user-form";
    }

    @PostMapping("/users/save")
    public String saveNewUser(@Validated(OnCreate.class) @ModelAttribute("userForm") AdminUserFormDto userForm,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addUserFormCommonAttributes(model); // Use helper
            model.addAttribute("isEditMode", false);
            return "admin-user-form";
        }
        try {
            adminService.createUser(userForm);
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully: " + userForm.getEmail());
        } catch (IllegalArgumentException e) {
            addUserFormCommonAttributes(model); // Use helper
            model.addAttribute("isEditMode", false);
            model.addAttribute("pageErrorMessage", e.getMessage());
            return "admin-user-form";
        } catch (Exception e) {
            addUserFormCommonAttributes(model); // Use helper
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
            addUserFormCommonAttributes(model); // Use helper
            model.addAttribute("isEditMode", true);
            return "admin-user-form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found with ID: " + userId + ". " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/users/update")
    public String updateUser(@Validated(OnUpdate.class) @ModelAttribute("userForm") AdminUserFormDto userForm,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (userForm.getUserId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "User ID is missing for update.");
            return "redirect:/admin/users";
        }

        if (userForm.getPassword() != null && !userForm.getPassword().isEmpty() && userForm.getPassword().length() < 8) {
            bindingResult.rejectValue("password", "Size.userForm.password", "Password must be at least 8 characters long if provided.");
        }

        if (bindingResult.hasErrors()) {
            addUserFormCommonAttributes(model); // Use helper
            model.addAttribute("isEditMode", true);
            return "admin-user-form";
        }

        try {
            adminService.updateUser(userForm);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully: " + userForm.getEmail());
        } catch (IllegalArgumentException e) {
            addUserFormCommonAttributes(model); // Use helper
            model.addAttribute("isEditMode", true);
            model.addAttribute("pageErrorMessage", e.getMessage());
            return "admin-user-form";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            addUserFormCommonAttributes(model); // Use helper
            model.addAttribute("isEditMode", true);
            String message = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : "Invalid data";
            model.addAttribute("pageErrorMessage", "Data integrity violation: " + message);
            return "admin-user-form";
        } catch (Exception e) {
            addUserFormCommonAttributes(model); // Use helper
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