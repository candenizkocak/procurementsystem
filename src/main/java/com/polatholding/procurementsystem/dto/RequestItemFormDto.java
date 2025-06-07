package com.polatholding.procurementsystem.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RequestItemFormDto {
    private String itemName;
    private int quantity;
    private BigDecimal unitPrice;
    private String description;

    // We use IDs for dropdown selections
    private Integer supplierId;
    private Integer unitId;
}