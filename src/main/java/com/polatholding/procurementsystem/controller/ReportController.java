package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('Finance Officer', 'ProcurementManager')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/budget-status")
    public String showBudgetStatusReport(Model model) {
        model.addAttribute("reportData", reportService.getBudgetStatusReport());
        return "report-budget-status";
    }
}