package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.BudgetStatusDto;
import java.util.List;

public interface ReportService {
    List<BudgetStatusDto> getBudgetStatusReport();
}