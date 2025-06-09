package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.dto.PurchaseRequestDto;
import com.polatholding.procurementsystem.service.ApprovalService;
import com.polatholding.procurementsystem.service.PurchaseRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/approvals")
public class ApprovalController {

    private final PurchaseRequestService purchaseRequestService;
    private final ApprovalService approvalService;

    public ApprovalController(PurchaseRequestService purchaseRequestService, ApprovalService approvalService) {
        this.purchaseRequestService = purchaseRequestService;
        this.approvalService = approvalService;
    }

    @GetMapping
    public String showMyApprovals(Model model, Principal principal) {
        String userEmail = principal.getName();
        List<PurchaseRequestDto> approvalRequests = purchaseRequestService.getPendingApprovalsForUser(userEmail);
        model.addAttribute("approvalRequests", approvalRequests);
        return "approvals";
    }

    @PostMapping("/process")
    public String processApprovalDecision(@RequestParam int requestId,
                                          @RequestParam String decision,
                                          @RequestParam(required = false) String rejectReason,
                                          Principal principal,
                                          RedirectAttributes redirectAttributes) {

        try {
            String userEmail = principal.getName();
            approvalService.processDecision(requestId, userEmail, decision, rejectReason);
            redirectAttributes.addFlashAttribute("successMessage", "Decision processed successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error processing decision: " + e.getMessage());
        }

        return "redirect:/approvals";
    }

    @PostMapping("/return")
    public String returnRequestForEdit(@RequestParam int requestId,
                                       @RequestParam String comments,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        try {
            String userEmail = principal.getName();
            approvalService.returnForEdit(requestId, userEmail, comments);
            redirectAttributes.addFlashAttribute("successMessage", "Request #" + requestId + " has been returned to the creator.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error returning request: " + e.getMessage());
        }

        return "redirect:/approvals";
    }
}