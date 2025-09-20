package com.minibank.fx.infrastructure.providers;

import com.minibank.fx.domain.ExchangeRate;
import com.minibank.fx.domain.FXRateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "fx.providers.xe.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "fx.providers.xe")
public class XEComProvider implements FXRateProvider {
    private static final Logger logger = LoggerFactory.getLogger(XEComProvider.class);
    private static final String PROVIDER_NAME = "XE_COM";
    private static final int PRIORITY = 0; // Highest priority
    private static final BigDecimal DEFAULT_SPREAD = new BigDecimal("0.0008"); // 0.08%

    private final RestTemplate restTemplate;
    private final Map<String, XEResponse> cachedResponses = new ConcurrentHashMap<>();
    private volatile ProviderStatus status = ProviderStatus.ACTIVE;
    private volatile Instant lastUpdate = Instant.now();

    private String accountId;
    private String apiKey;
    private String baseUrl = "https://xecdapi.xe.com/v1/convert_from";
    private boolean enabled = true;

    public XEComProvider(RestTemplate restTemplate) {
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
        return enabled && status == ProviderStatus.ACTIVE && accountId != null && apiKey != null;
    }

    @Override
    public Optional<ExchangeRate> getRate(String baseCurrency, String quoteCurrency) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        try {
            XEResponse response = getOrFetchRate(baseCurrency, quoteCurrency);
            if (response == null || response.to == null || response.to.isEmpty()) {
                return Optional.empty();
            }

            XEConversionRate conversionRate = response.to.get(0);
            BigDecimal rate = conversionRate.mid;
            
            ExchangeRate exchangeRate = ExchangeRate.create(
                baseCurrency,
                quoteCurrency,
                rate,
                DEFAULT_SPREAD,
                PROVIDER_NAME,
                Instant.now(),
                Instant.now().plus(30, ChronoUnit.SECONDS) // XE.com rates are very fresh
            );

            logger.debug("Retrieved XE.com rate for {}/{}: {}", baseCurrency, quoteCurrency, rate);
            return Optional.of(exchangeRate);

        } catch (Exception e) {
            logger.error("Failed to retrieve rate from XE.com for {}/{}: {}", 
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

        // XE.com API is more efficient for individual pairs, so we'll fetch common pairs
        List<String[]> commonPairs = List.of(
            new String[]{"USD", "EUR"}, new String[]{"USD", "GBP"}, new String[]{"USD", "JPY"},
            new String[]{"USD", "CHF"}, new String[]{"USD", "CAD"}, new String[]{"USD", "AUD"},
            new String[]{"EUR", "GBP"}, new String[]{"EUR", "JPY"}, new String[]{"GBP", "JPY"}
        );

        return commonPairs.stream()
            .map(pair -> getRate(pair[0], pair[1]))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    @Override
    public boolean supports(String baseCurrency, String quoteCurrency) {
        // XE.com supports most major currency pairs
        List<String> supportedCurrencies = List.of(
            "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "INR", "BRL", "MXN",
            "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "TRY", "ZAR", "RUB", "KRW", "SGD",
            "HKD", "NZD", "ILS", "CLP", "PHP", "COP", "ARS", "PEN"
        );
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

    private XEResponse getOrFetchRate(String baseCurrency, String quoteCurrency) {
        String cacheKey = baseCurrency + "/" + quoteCurrency;
        XEResponse cached = cachedResponses.get(cacheKey);
        if (cached != null && cached.timestamp > System.currentTimeMillis() - 30000) { // 30 second cache
            return cached;
        }

        try {
            String url = String.format("%s.json?from=%s&to=%s&amount=1", 
                                     baseUrl, baseCurrency, quoteCurrency);
            
            HttpHeaders headers = new HttpHeaders();
            String auth = accountId + ":" + apiKey;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<XEResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, XEResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                XEResponse data = response.getBody();
                data.timestamp = System.currentTimeMillis();
                
                cachedResponses.put(cacheKey, data);
                lastUpdate = Instant.now();
                status = ProviderStatus.ACTIVE;
                
                logger.debug("Fetched rate from XE.com API for {}/{}", baseCurrency, quoteCurrency);
                return data;
            } else {
                logger.warn("XE.com API returned non-success status: {}", response.getStatusCode());
                status = ProviderStatus.ERROR;
                return null;
            }

        } catch (Exception e) {
            logger.error("Failed to fetch rate from XE.com API: {}", e.getMessage());
            status = ProviderStatus.ERROR;
            return null;
        }
    }

    public void clearCache() {
        cachedResponses.clear();
        logger.info("Cleared XE.com FX rate cache");
    }

    public void setStatus(ProviderStatus status) {
        this.status = status;
        logger.info("XE.com provider status changed to: {}", status);
    }

    // Configuration properties setters
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    static class XEResponse {
        public String terms;
        public String privacy;
        public String from;
        public double amount;
        public List<XEConversionRate> to;
        public long timestamp; // Internal field for caching

        public XEResponse() {
            this.to = List.of();
        }
    }

    static class XEConversionRate {
        public String quotecurrency;
        public BigDecimal mid;
        public String timestamp;
    }
}