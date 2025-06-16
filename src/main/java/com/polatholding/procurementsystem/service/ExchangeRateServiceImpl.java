package com.polatholding.procurementsystem.service;

import com.polatholding.procurementsystem.dto.ExchangeRateApiResponse;
import com.polatholding.procurementsystem.model.Currency;
import com.polatholding.procurementsystem.model.ExchangeRate;
import com.polatholding.procurementsystem.repository.CurrencyRepository;
import com.polatholding.procurementsystem.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateServiceImpl.class);

    private final RestTemplate restTemplate;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final com.polatholding.procurementsystem.repository.DatabaseHelperRepository dbHelper;

    @Value("${exchange.rate.api.url}")
    private String apiUrl;

    public ExchangeRateServiceImpl(RestTemplate restTemplate,
                                   CurrencyRepository currencyRepository,
                                   ExchangeRateRepository exchangeRateRepository,
                                   com.polatholding.procurementsystem.repository.DatabaseHelperRepository dbHelper) {
        this.restTemplate = restTemplate;
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.dbHelper = dbHelper;
    }

    @Override
    @Scheduled(cron = "0 26 14 * * ?")
    @Transactional
    public void updateDailyExchangeRates() {
        log.info("Starting daily exchange rate update");
        ExchangeRateApiResponse response;
        try {
            response = restTemplate.getForObject(apiUrl, ExchangeRateApiResponse.class);
        } catch (RestClientException ex) {
            log.warn("Failed to fetch exchange rates from API", ex);
            return;
        }
        if (response == null || response.getRates() == null) {
            log.warn("Exchange rate API returned no data");
            return;
        }
        LocalDate today = LocalDate.now(); // This is the date we want to use

        BigDecimal usdRateFromApi = response.getRates().get("USD"); // e.g., TRY per 1 USD
        BigDecimal eurRateFromApi = response.getRates().get("EUR"); // e.g., TRY per 1 EUR

        // Assuming API gives rates as X / TRY (e.g. USD/TRY, EUR/TRY)
        // and we want to store them as units of TRY for 1 unit of foreign currency.
        // The API gives: 1 TRY = X USD. So, 1 USD = 1/X TRY.
        // Or if the API gives: 1 USD = X TRY, then that's what we store.
        // Your current code inverts:
        // BigDecimal usdAgainstTry = (usdRate != null && usdRate.compareTo(BigDecimal.ZERO) != 0)
        //    ? BigDecimal.ONE.divide(usdRate, 6, BigDecimal.ROUND_HALF_UP)
        //    : null;
        // This implies the API gives rates like 1 TRY = 0.03 USD, so 1 USD = 1/0.03 TRY.
        // Let's stick to your existing inversion logic.

        BigDecimal usdRateInTry = (usdRateFromApi != null && usdRateFromApi.compareTo(BigDecimal.ZERO) != 0)
                ? BigDecimal.ONE.divide(usdRateFromApi, 6, BigDecimal.ROUND_HALF_UP)
                : null;
        BigDecimal eurRateInTry = (eurRateFromApi != null && eurRateFromApi.compareTo(BigDecimal.ZERO) != 0)
                ? BigDecimal.ONE.divide(eurRateFromApi, 6, BigDecimal.ROUND_HALF_UP)
                : null;

        saveRate("USD", usdRateInTry, today);
        saveRate("EUR", eurRateInTry, today);
        saveRate("TRY", BigDecimal.ONE, today); // TRY to TRY is always 1

        log.info("Finished updating rates for {}: USD/TRY={}, EUR/TRY={}", today, usdRateInTry, eurRateInTry);
    }

    private void saveRate(String currencyCode, BigDecimal rate, LocalDate date) { // 'date' is now used
        if (rate == null) {
            log.warn("No rate value for currency {} on {}", currencyCode, date);
            return;
        }
        currencyRepository.findByCurrencyCode(currencyCode).ifPresentOrElse(currency -> {
            // Pass the date to the dbHelper method
            dbHelper.updateExchangeRate(currencyCode, rate, date);
            log.debug("Saved/updated exchange rate for {} on {} using procedure", currencyCode, date);
        }, () -> log.warn("Currency not found for code {}", currencyCode));
    }
}

