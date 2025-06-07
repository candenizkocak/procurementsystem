package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.dto.PurchaseRequestDetailDto;
import com.polatholding.procurementsystem.dto.PurchaseRequestFormDto;
import com.polatholding.procurementsystem.service.PurchaseRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/requests")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    public PurchaseRequestController(PurchaseRequestService purchaseRequestService) {
        this.purchaseRequestService = purchaseRequestService;
    }

    @GetMapping("/new")
    public String showNewRequestForm(Model model) {
        model.addAttribute("formData", purchaseRequestService.getNewRequestFormData());
        model.addAttribute("requestForm", new PurchaseRequestFormDto());
        return "request-form";
    }

    @PostMapping("/save")
    public String saveNewRequest(@ModelAttribute("requestForm") PurchaseRequestFormDto formDto,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            purchaseRequestService.saveNewRequest(formDto, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Purchase Request created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating request: " + e.getMessage());
            return "redirect:/requests/new";
        }
        return "redirect:/dashboard";
    }

    // NEW METHOD for viewing details
    @GetMapping("/{id}")
    public String viewRequestDetails(@PathVariable("id") Integer id, Model model) {
        PurchaseRequestDetailDto requestDetails = purchaseRequestService.getRequestDetailsById(id);
        model.addAttribute("request", requestDetails);
        return "request-details"; // Name of the new details HTML page
    }
}