package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.config.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/notifications")
public class NotificationViewController {

    @GetMapping("/all")
    public String viewAllNotifications(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        model.addAttribute("userId", userDetails.getUserId());
        model.addAttribute("username", userDetails.getUsername());
        // Add any additional user attributes needed for the template

        return "all-notifications";
    }
}
