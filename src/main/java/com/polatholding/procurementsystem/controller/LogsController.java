package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.service.RequestHistoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/logs")
@PreAuthorize("hasRole('Admin')")
public class LogsController {

    private final RequestHistoryService requestHistoryService;

    public LogsController(RequestHistoryService requestHistoryService) {
        this.requestHistoryService = requestHistoryService;
    }

    @GetMapping
    public String showLogs(Model model) {
        model.addAttribute("logs", requestHistoryService.getAllHistory());
        return "admin-logs";
    }
}
