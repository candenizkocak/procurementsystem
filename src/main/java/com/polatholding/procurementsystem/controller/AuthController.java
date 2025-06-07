package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.dto.PurchaseRequestDto;
import com.polatholding.procurementsystem.service.PurchaseRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
    public String dashboard(Model model, Principal principal) {
        // This now calls a method on the INTERFACE, which is better practice.
        // The implementation will handle the logic.
        List<PurchaseRequestDto> requests = purchaseRequestService.getRequestsForUser(principal.getName());

        model.addAttribute("requests", requests);

        return "dashboard";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }
}