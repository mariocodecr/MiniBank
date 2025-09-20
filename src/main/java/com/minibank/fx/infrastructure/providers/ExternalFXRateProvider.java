package com.minibank.fx.infrastructure.providers;

import com.minibank.fx.domain.ExchangeRate;
import com.minibank.fx.domain.FXRateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "fx.providers.external.enabled", havingValue = "true")
public class ExternalFXRateProvider implements FXRateProvider {
    private static final Logger logger = LoggerFactory.getLogger(ExternalFXRateProvider.class);
    private static final String PROVIDER_NAME = "EXTERNAL_API";
    private static final int PRIORITY = 1; // High priority
    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/";
    private static final BigDecimal DEFAULT_SPREAD = new BigDecimal("0.001"); // 0.1%

    private final RestTemplate restTemplate;
    private final Map<String, ExchangeRateResponse> cachedResponses = new ConcurrentHashMap<>();
    private volatile ProviderStatus status = ProviderStatus.ACTIVE;
    private volatile Instant lastUpdate = Instant.now();

    public ExternalFXRateProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isEnabled() {
        return status == ProviderStatus.ACTIVE;
    }

    @Override
    public Optional<ExchangeRate> getRate(String baseCurrency, String quoteCurrency) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        try {
            ExchangeRateResponse response = getOrFetchRates(baseCurrency);
            if (response == null || !response.rates.containsKey(quoteCurrency)) {
                return Optional.empty();
            }

            BigDecimal rate = response.rates.get(quoteCurrency);
            ExchangeRate exchangeRate = ExchangeRate.create(
                baseCurrency,
                quoteCurrency,
                rate,
                DEFAULT_SPREAD,
                PROVIDER_NAME,
                Instant.now(),
                Instant.now().plus(1, ChronoUnit.MINUTES)
            );

            logger.debug("Retrieved external rate for {}/{}: {}", baseCurrency, quoteCurrency, rate);
            return Optional.of(exchangeRate);

        } catch (Exception e) {
            logger.error("Failed to retrieve rate for {}/{}: {}", baseCurrency, quoteCurrency, e.getMessage());
            status = ProviderStatus.ERROR;
            return Optional.empty();
        }
    }

    @Override
    public List<ExchangeRate> getAllRates() {
        if (!isEnabled()) {
            return List.of();
        }

        // For simplicity, return rates for major currencies from USD base
        String baseCurrency = "USD";
        try {
            ExchangeRateResponse response = getOrFetchRates(baseCurrency);
            if (response == null) {
                return List.of();
            }

            return response.rates.entrySet().stream()
                .map(entry -> ExchangeRate.create(
                    baseCurrency,
                    entry.getKey(),
                    entry.getValue(),
                    DEFAULT_SPREAD,
                    PROVIDER_NAME,
                    Instant.now(),
                    Instant.now().plus(1, ChronoUnit.MINUTES)
                ))
                .toList();

        } catch (Exception e) {
            logger.error("Failed to retrieve all rates: {}", e.getMessage());
            status = ProviderStatus.ERROR;
            return List.of();
        }
    }

    @Override
    public boolean supports(String baseCurrency, String quoteCurrency) {
        // This provider supports most major currencies, but we'd need to check the API
        // For simplicity, assume it supports common currency pairs
        List<String> supportedCurrencies = List.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD");
        return supportedCurrencies.contains(baseCurrency) && supportedCurrencies.contains(quoteCurrency);
    }

    @Override
    public Instant getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public ProviderStatus getStatus() {
        return status;
    }

    private ExchangeRateResponse getOrFetchRates(String baseCurrency) {
        ExchangeRateResponse cached = cachedResponses.get(baseCurrency);
        if (cached != null && cached.timestamp > System.currentTimeMillis() - 60000) { // 1 minute cache
            return cached;
        }

        try {
            String url = API_URL + baseCurrency;
            ResponseEntity<ExchangeRateResponse> response = restTemplate.getForEntity(url, ExchangeRateResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ExchangeRateResponse data = response.getBody();
                data.timestamp = System.currentTimeMillis();
                cachedResponses.put(baseCurrency, data);
                lastUpdate = Instant.now();
                status = ProviderStatus.ACTIVE;
                
                logger.debug("Fetched rates from external API for base currency: {}", baseCurrency);
                return data;
            } else {
                logger.warn("External API returned non-success status: {}", response.getStatusCode());
                status = ProviderStatus.ERROR;
                return null;
            }

        } catch (Exception e) {
            logger.error("Failed to fetch rates from external API: {}", e.getMessage());
            status = ProviderStatus.ERROR;
            return null;
        }
    }

    public void clearCache() {
        cachedResponses.clear();
        logger.info("Cleared external FX rate cache");
    }

    public void setStatus(ProviderStatus status) {
        this.status = status;
        logger.info("External FX provider status changed to: {}", status);
    }

    static class ExchangeRateResponse {
        public String base;
        public String date;
        public Map<String, BigDecimal> rates;
        public long timestamp; // Internal field for caching

        public ExchangeRateResponse() {
            this.rates = new ConcurrentHashMap<>();
        }
    }
}