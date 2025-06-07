package com.polatholding.procurementsystem.dto;

import com.polatholding.procurementsystem.model.BudgetCode;
import com.polatholding.procurementsystem.model.Currency;
import com.polatholding.procurementsystem.model.Supplier;
import com.polatholding.procurementsystem.model.Unit;
import lombok.Data;

import java.util.List;

@Data
public class NewRequestFormInitDto {
    private List<BudgetCode> budgetCodes;
    private List<Currency> currencies;
    private List<Supplier> suppliers;
    private List<Unit> units;
}