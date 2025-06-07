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

    /**
     * Finds the most recent exchange rate for a given currency on or before a specific date.
     * This is crucial for finding the rate that was active when a request was created.
     * @param currencyId The ID of the currency.
     * @param date The date to find the rate for.
     * @return An Optional containing the found ExchangeRate.
     */
    @Query("SELECT er FROM ExchangeRate er WHERE er.currency.id = :currencyId AND er.date <= :date ORDER BY er.date DESC, er.exchangeRateId DESC LIMIT 1")
    Optional<ExchangeRate> findTopByCurrencyIdAndDateLessThanEqualOrderByDateDesc(
            @Param("currencyId") Integer currencyId,
            @Param("date") LocalDate date
    );
}