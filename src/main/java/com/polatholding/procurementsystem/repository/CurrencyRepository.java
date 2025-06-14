package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Integer> {
    java.util.Optional<Currency> findByCurrencyCode(String currencyCode);
}