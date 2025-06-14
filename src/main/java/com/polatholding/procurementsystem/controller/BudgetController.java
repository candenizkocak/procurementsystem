package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.dto.BudgetFormDto;
import com.polatholding.procurementsystem.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/budgets")
@PreAuthorize("hasAnyRole('Finance Officer','Auditor')")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public String showBudgetList(Model model) {
        model.addAttribute("budgets", budgetService.getAllBudgets());
        return "budgets"; // budgets.html
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('Finance Officer')")
    public String showNewBudgetForm(Model model) {
        model.addAttribute("budgetFormDto", new BudgetFormDto());
        model.addAttribute("departments", budgetService.getAllDepartments());
        model.addAttribute("isEditMode", false);
        return "budget-form"; // budget-form.html
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('Finance Officer')")
    public String saveNewBudget(@Valid @ModelAttribute("budgetFormDto") BudgetFormDto formDto,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", budgetService.getAllDepartments());
            model.addAttribute("isEditMode", false);
            return "budget-form";
        }
        budgetService.createNewBudget(formDto);
        redirectAttributes.addFlashAttribute("successMessage", "New budget created successfully.");
        return "redirect:/budgets";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('Finance Officer')")
    public String showEditBudgetForm(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("budgetFormDto", budgetService.getBudgetFormById(id));
        model.addAttribute("departments", budgetService.getAllDepartments());
        model.addAttribute("isEditMode", true);
        return "budget-form";
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasRole('Finance Officer')")
    public String updateBudget(@PathVariable("id") Integer id,
                               @Valid @ModelAttribute("budgetFormDto") BudgetFormDto formDto,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", budgetService.getAllDepartments());
            model.addAttribute("isEditMode", true);
            return "budget-form";
        }
        budgetService.updateBudget(id, formDto);
        redirectAttributes.addFlashAttribute("successMessage", "Budget updated successfully.");
        return "redirect:/budgets";
    }
}