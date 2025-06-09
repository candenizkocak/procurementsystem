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

    @Value("${exchange.rate.api.url}")
    private String apiUrl;

    public ExchangeRateServiceImpl(RestTemplate restTemplate,
                                   CurrencyRepository currencyRepository,
                                   ExchangeRateRepository exchangeRateRepository) {
        this.restTemplate = restTemplate;
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @Override
    @Scheduled(cron = "0 0 10 * * ?")
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
        LocalDate today = LocalDate.now();

        // Get USD and EUR rates from API (these are TRY against USD/EUR)
        BigDecimal usdRate = response.getRates().get("USD");
        BigDecimal eurRate = response.getRates().get("EUR");

        // Invert the rates to get USD/EUR against TRY
        BigDecimal usdAgainstTry = (usdRate != null && usdRate.compareTo(BigDecimal.ZERO) != 0)
            ? BigDecimal.ONE.divide(usdRate, 6, BigDecimal.ROUND_HALF_UP)
            : null;
        BigDecimal eurAgainstTry = (eurRate != null && eurRate.compareTo(BigDecimal.ZERO) != 0)
            ? BigDecimal.ONE.divide(eurRate, 6, BigDecimal.ROUND_HALF_UP)
            : null;

        // Save the inverted rates
        saveRate("USD", usdAgainstTry, today);
        saveRate("EUR", eurAgainstTry, today);
        saveRate("TRY", BigDecimal.ONE, today);

        log.info("Finished updating rates: USD/TRY={}, EUR/TRY={}", usdAgainstTry, eurAgainstTry);
    }

    private void saveRate(String currencyCode, BigDecimal rate, LocalDate date) {
        if (rate == null) {
            log.warn("No rate value for currency {} on {}", currencyCode, date);
            return;
        }
        currencyRepository.findByCurrencyCode(currencyCode).ifPresentOrElse(currency -> {
            // Check if an exchange rate already exists for this currency and date
            ExchangeRate exchangeRate = exchangeRateRepository
                .findByCurrencyAndDate(currency, date)
                .orElse(new ExchangeRate());

            exchangeRate.setCurrency(currency);
            exchangeRate.setRate(rate);
            exchangeRate.setDate(date);
            exchangeRateRepository.save(exchangeRate);
            log.debug("Saved/updated exchange rate for {} on {}: {}", currencyCode, date, rate);
        }, () -> log.warn("Currency not found for code {}", currencyCode));
    }
}

