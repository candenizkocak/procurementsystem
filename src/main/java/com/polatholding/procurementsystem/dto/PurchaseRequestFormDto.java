package com.polatholding.procurementsystem.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class PurchaseRequestFormDto {

    // We use IDs for dropdown selections
    private Integer budgetCodeId;
    private Integer currencyId;

    // A list to hold all the items added in the form
    private List<RequestItemFormDto> items = new ArrayList<>();

    public PurchaseRequestFormDto() {
        // Initialize with one empty item row for the form to display initially
        this.items.add(new RequestItemFormDto());
    }
}