package com.polatholding.procurementsystem.repository;

import com.polatholding.procurementsystem.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Integer> {

    @Query("SELECT er FROM ExchangeRate er WHERE er.currency.id = :currencyId AND er.date <= :date ORDER BY er.date DESC, er.exchangeRateId DESC LIMIT 1")
    Optional<ExchangeRate> findTopByCurrencyIdAndDateLessThanEqualOrderByDateDesc(
            @Param("currencyId") Integer currencyId,
            @Param("date") LocalDate date
    );

    Optional<ExchangeRate> findByCurrencyAndDate(
            com.polatholding.procurementsystem.model.Currency currency,
            LocalDate date
    );
}

