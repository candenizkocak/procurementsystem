package com.polatholding.procurementsystem.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ExchangeRateApiResponse {
    private String result;
    private String base_code;
    private Map<String, BigDecimal> rates;
}