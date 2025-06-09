package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.dto.PurchaseRequestDto;
import com.polatholding.procurementsystem.service.PurchaseRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class AuthController {

    private final PurchaseRequestService purchaseRequestService;

    public AuthController(PurchaseRequestService purchaseRequestService) {
        this.purchaseRequestService = purchaseRequestService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(name = "q", required = false) String query, Model model, Principal principal) {
        List<PurchaseRequestDto> requests;
        if (query != null && !query.trim().isEmpty()) {
            // If there's a search query, use the search method
            requests = purchaseRequestService.searchUserRequests(principal.getName(), query);
            model.addAttribute("searchTerm", query);
        } else {
            // Otherwise, get requests normally
            requests = purchaseRequestService.getRequestsForUser(principal.getName());
        }
        model.addAttribute("requests", requests);
        return "dashboard";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }
}