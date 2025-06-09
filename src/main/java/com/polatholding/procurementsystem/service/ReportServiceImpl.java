package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.BudgetStatusDto;
import com.polatholding.procurementsystem.model.BudgetCode;
import com.polatholding.procurementsystem.repository.BudgetCodeRepository;
import com.polatholding.procurementsystem.repository.PurchaseRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final BudgetCodeRepository budgetCodeRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;

    public ReportServiceImpl(BudgetCodeRepository budgetCodeRepository, PurchaseRequestRepository purchaseRequestRepository) {
        this.budgetCodeRepository = budgetCodeRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetStatusDto> getBudgetStatusReport() {
        List<BudgetCode> allBudgets = budgetCodeRepository.findAll();
        List<BudgetStatusDto> report = new ArrayList<>();

        for (BudgetCode budget : allBudgets) {
            BudgetStatusDto dto = new BudgetStatusDto();
            dto.setDepartmentName(budget.getDepartment().getDepartmentName());
            dto.setBudgetCode(budget.getCode());
            dto.setYear(budget.getYear());

            BigDecimal consumedAmount = purchaseRequestRepository.getConsumedAmountForBudget(budget.getBudgetCodeId());
            BigDecimal currentAmount = budget.getBudgetAmount();
            BigDecimal initialAmount = currentAmount.add(consumedAmount);

            dto.setInitialAmount(initialAmount);
            dto.setConsumedAmount(consumedAmount);
            dto.setRemainingAmount(currentAmount);

            if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = consumedAmount.divide(initialAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                dto.setConsumptionPercentage(percentage.doubleValue());
            } else {
                dto.setConsumptionPercentage(0.0);
            }

            report.add(dto);
        }

        return report;
    }
}