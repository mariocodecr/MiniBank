package com.minibank.fx.infrastructure.providers;

import com.minibank.fx.domain.ExchangeRate;
import com.minibank.fx.domain.FXRateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
@ConditionalOnProperty(name = "fx.providers.currencylayer.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "fx.providers.currencylayer")
public class CurrencyLayerProvider implements FXRateProvider {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyLayerProvider.class);
    private static final String PROVIDER_NAME = "CURRENCY_LAYER";
    private static final int PRIORITY = 2; // Secondary priority
    private static final BigDecimal DEFAULT_SPREAD = new BigDecimal("0.0012"); // 0.12%

    private final RestTemplate restTemplate;
    private final Map<String, CurrencyLayerResponse> cachedResponses = new ConcurrentHashMap<>();
    private volatile ProviderStatus status = ProviderStatus.ACTIVE;
    private volatile Instant lastUpdate = Instant.now();

    private String apiKey;
    private String baseUrl = "http://api.currencylayer.com/live";
    private boolean enabled = true;

    public CurrencyLayerProvider(RestTemplate restTemplate) {
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
        return enabled && status == ProviderStatus.ACTIVE && apiKey != null;
    }

    @Override
    public Optional<ExchangeRate> getRate(String baseCurrency, String quoteCurrency) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        try {
            CurrencyLayerResponse response = getOrFetchRates(baseCurrency);
            if (response == null || !response.success) {
                return Optional.empty();
            }

            String quoteKey = baseCurrency + quoteCurrency;
            if (!response.quotes.containsKey(quoteKey)) {
                return Optional.empty();
            }

            BigDecimal rate = response.quotes.get(quoteKey);
            ExchangeRate exchangeRate = ExchangeRate.create(
                baseCurrency,
                quoteCurrency,
                rate,
                DEFAULT_SPREAD,
                PROVIDER_NAME,
                Instant.now(),
                Instant.now().plus(2, ChronoUnit.MINUTES)
            );

            logger.debug("Retrieved CurrencyLayer rate for {}/{}: {}", baseCurrency, quoteCurrency, rate);
            return Optional.of(exchangeRate);

        } catch (Exception e) {
            logger.error("Failed to retrieve rate from CurrencyLayer for {}/{}: {}", 
                        baseCurrency, quoteCurrency, e.getMessage());
            status = ProviderStatus.ERROR;
            return Optional.empty();
        }
    }

    @Override
    public List<ExchangeRate> getAllRates() {
        if (!isEnabled()) {
            return List.of();
        }

        String baseCurrency = "USD";
        try {
            CurrencyLayerResponse response = getOrFetchRates(baseCurrency);
            if (response == null || !response.success) {
                return List.of();
            }

            return response.quotes.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(baseCurrency))
                .map(entry -> {
                    String quoteCurrency = entry.getKey().substring(3); // Remove base currency prefix
                    return ExchangeRate.create(
                        baseCurrency,
                        quoteCurrency,
                        entry.getValue(),
                        DEFAULT_SPREAD,
                        PROVIDER_NAME,
                        Instant.now(),
                        Instant.now().plus(2, ChronoUnit.MINUTES)
                    );
                })
                .toList();

        } catch (Exception e) {
            logger.error("Failed to retrieve all rates from CurrencyLayer: {}", e.getMessage());
            status = ProviderStatus.ERROR;
            return List.of();
        }
    }

    @Override
    public boolean supports(String baseCurrency, String quoteCurrency) {
        // CurrencyLayer supports USD as base for most currencies
        if (!"USD".equals(baseCurrency)) {
            return false;
        }

        List<String> supportedCurrencies = List.of(
            "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "INR", "BRL", "MXN",
            "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "TRY", "ZAR", "RUB"
        );
        return supportedCurrencies.contains(quoteCurrency);
    }

    @Override
    public Instant getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public ProviderStatus getStatus() {
        return status;
    }

    private CurrencyLayerResponse getOrFetchRates(String baseCurrency) {
        if (!"USD".equals(baseCurrency)) {
            logger.debug("CurrencyLayer only supports USD as base currency");
            return null;
        }

        CurrencyLayerResponse cached = cachedResponses.get(baseCurrency);
        if (cached != null && cached.timestamp > System.currentTimeMillis() - 120000) { // 2 minute cache
            return cached;
        }

        try {
            String url = String.format("%s?access_key=%s&source=%s&format=1", 
                                     baseUrl, apiKey, baseCurrency);
            
            ResponseEntity<CurrencyLayerResponse> response = restTemplate.getForEntity(url, CurrencyLayerResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                CurrencyLayerResponse data = response.getBody();
                data.timestamp = System.currentTimeMillis();
                
                if (data.success) {
                    cachedResponses.put(baseCurrency, data);
                    lastUpdate = Instant.now();
                    status = ProviderStatus.ACTIVE;
                    
                    logger.debug("Fetched rates from CurrencyLayer API for base currency: {}", baseCurrency);
                    return data;
                } else {
                    logger.warn("CurrencyLayer API returned error: {}", data.error != null ? data.error.info : "Unknown error");
                    status = ProviderStatus.ERROR;
                    return null;
                }
            } else {
                logger.warn("CurrencyLayer API returned non-success status: {}", response.getStatusCode());
                status = ProviderStatus.ERROR;
                return null;
            }

        } catch (Exception e) {
            logger.error("Failed to fetch rates from CurrencyLayer API: {}", e.getMessage());
            status = ProviderStatus.ERROR;
            return null;
        }
    }

    public void clearCache() {
        cachedResponses.clear();
        logger.info("Cleared CurrencyLayer FX rate cache");
    }

    public void setStatus(ProviderStatus status) {
        this.status = status;
        logger.info("CurrencyLayer provider status changed to: {}", status);
    }

    // Configuration properties setters
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    static class CurrencyLayerResponse {
        public boolean success;
        public String terms;
        public String privacy;
        public long timestamp; // Internal field for caching
        public String source;
        public Map<String, BigDecimal> quotes;
        public CurrencyLayerError error;

        public CurrencyLayerResponse() {
            this.quotes = new ConcurrentHashMap<>();
        }
    }

    static class CurrencyLayerError {
        public int code;
        public String info;
    }
}