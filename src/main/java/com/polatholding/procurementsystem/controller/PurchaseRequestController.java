package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.dto.PurchaseRequestDetailDto;
import com.polatholding.procurementsystem.dto.PurchaseRequestFormDto;
import com.polatholding.procurementsystem.service.PurchaseRequestService;
import org.springframework.security.access.AccessDeniedException;
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
    public String showNewRequestForm(Model model, Principal principal) {
        model.addAttribute("formData", purchaseRequestService.getNewRequestFormData(principal.getName()));
        model.addAttribute("requestForm", new PurchaseRequestFormDto());
        // For the form title and action URL
        model.addAttribute("isEditMode", false);
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

    /**
     * NEW: Displays the request form in edit mode, pre-filled with data.
     */
    @GetMapping("/{id}/edit")
    public String showEditRequestForm(@PathVariable("id") Integer id, Model model, Principal principal) {
        // Security check: Ensure the user is the creator of the request
        PurchaseRequestDetailDto requestDetails = purchaseRequestService.getRequestDetailsById(id);
        if (!requestDetails.getCreatorFullName().equals(purchaseRequestService.getUserFullName(principal.getName()))) {
            throw new AccessDeniedException("You are not authorized to edit this request.");
        }

        model.addAttribute("formData", purchaseRequestService.getNewRequestFormData(principal.getName()));
        model.addAttribute("requestForm", purchaseRequestService.getRequestFormById(id));
        model.addAttribute("isEditMode", true);
        model.addAttribute("requestId", id);
        return "request-form";
    }

    /**
     * NEW: Processes the form submission for an existing request.
     */
    @PostMapping("/{id}/update")
    public String updateRequest(@PathVariable("id") Integer id,
                                @ModelAttribute("requestForm") PurchaseRequestFormDto formDto,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            purchaseRequestService.updateRequest(id, formDto, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Request #" + id + " has been updated and resubmitted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating request: " + e.getMessage());
            return "redirect:/requests/" + id + "/edit";
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/{id}")
    public String viewRequestDetails(@PathVariable("id") Integer id, Model model) {
        PurchaseRequestDetailDto requestDetails = purchaseRequestService.getRequestDetailsById(id);
        model.addAttribute("request", requestDetails);
        return "request-details";
    }
}