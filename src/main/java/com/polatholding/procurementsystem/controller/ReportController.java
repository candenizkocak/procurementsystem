package com.polatholding.procurementsystem.controller;

import com.polatholding.procurementsystem.service.ReportService;
import com.polatholding.procurementsystem.service.BudgetService;
import com.polatholding.procurementsystem.service.PurchaseRequestService;
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
    private final BudgetService budgetService;
    private final PurchaseRequestService purchaseRequestService;

    public ReportController(ReportService reportService,
                           BudgetService budgetService,
                           PurchaseRequestService purchaseRequestService) {
        this.reportService = reportService;
        this.budgetService = budgetService;
        this.purchaseRequestService = purchaseRequestService;
    }

    @GetMapping("/budget-status")
    public String showBudgetStatusReport(Model model) {
        model.addAttribute("reportData", reportService.getBudgetStatusReport());
        return "report-budget-status";
    }

    @GetMapping("/budget-status/{id}/requests")
    public String showRequestsForBudget(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("budget", budgetService.getBudgetById(id));
        model.addAttribute("requests", purchaseRequestService.getRequestsByBudget(id));
        return "budget-requests";
    }
}