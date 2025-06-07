package com.polatholding.procurementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ExchangeRates")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ExchangeRateID")
    private Integer exchangeRateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CurrencyID", nullable = false)
    private Currency currency;

    @Column(name = "Rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal rate;

    @Column(name = "Date", nullable = false)
    private LocalDate date;
}