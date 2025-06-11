package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.BudgetStatusDto;
import com.polatholding.procurementsystem.model.BudgetCode;
import com.polatholding.procurementsystem.repository.BudgetCodeRepository;
import com.polatholding.procurementsystem.repository.PurchaseRequestRepository;
import com.polatholding.procurementsystem.repository.ExchangeRateRepository;
import com.polatholding.procurementsystem.model.PurchaseRequest;
import com.polatholding.procurementsystem.model.ExchangeRate;
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
    private final ExchangeRateRepository exchangeRateRepository;

    public ReportServiceImpl(BudgetCodeRepository budgetCodeRepository,
                             PurchaseRequestRepository purchaseRequestRepository,
                             ExchangeRateRepository exchangeRateRepository) {
        this.budgetCodeRepository = budgetCodeRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetStatusDto> getBudgetStatusReport() {
        List<BudgetCode> allBudgets = budgetCodeRepository.findAll();
        List<BudgetStatusDto> report = new ArrayList<>();

        for (BudgetCode budget : allBudgets) {
            BudgetStatusDto dto = new BudgetStatusDto();
            dto.setBudgetCodeId(budget.getBudgetCodeId());
            dto.setDepartmentName(budget.getDepartment().getDepartmentName());
            dto.setBudgetCode(budget.getCode());
            dto.setYear(budget.getYear());

            BigDecimal consumedAmount = BigDecimal.ZERO;
            for (PurchaseRequest pr : purchaseRequestRepository.findApprovedByBudget(budget.getBudgetCodeId())) {
                BigDecimal value;
                if ("TRY".equalsIgnoreCase(pr.getCurrency().getCurrencyCode())) {
                    value = pr.getNetAmount();
                } else {
                    ExchangeRate rate = exchangeRateRepository
                            .findTopByCurrencyIdAndDateLessThanEqualOrderByDateDesc(
                                    pr.getCurrency().getCurrencyId(),
                                    pr.getCreatedAt().toLocalDate())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Exchange rate not found for currency code: " + pr.getCurrency().getCurrencyCode()));
                    value = pr.getNetAmount().multiply(rate.getRate());
                }
                consumedAmount = consumedAmount.add(value);
            }
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