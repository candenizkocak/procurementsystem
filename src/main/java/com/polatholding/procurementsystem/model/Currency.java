package com.polatholding.procurementsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Currencies")
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CurrencyID")
    private Integer currencyId;

    @Column(name = "CurrencyCode", nullable = false, unique = true, length = 10)
    private String currencyCode;

    @Column(name = "CurrencyName", nullable = false, length = 50)
    private String currencyName;
}