package com.minibank.fx.infrastructure.providers;

import com.minibank.fx.domain.ExchangeRate;
import com.minibank.fx.domain.FXRateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "fx.providers.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockFXRateProvider implements FXRateProvider {
    private static final Logger logger = LoggerFactory.getLogger(MockFXRateProvider.class);
    private static final String PROVIDER_NAME = "MOCK";
    private static final int PRIORITY = 999; // Lowest priority
    private static final BigDecimal DEFAULT_SPREAD = new BigDecimal("0.002"); // 0.2%

    private final Map<String, BigDecimal> baseRates = new ConcurrentHashMap<>();
    private volatile ProviderStatus status = ProviderStatus.ACTIVE;
    private volatile Instant lastUpdate = Instant.now();

    public MockFXRateProvider() {
        initializeBaseRates();
    }

    private void initializeBaseRates() {
        baseRates.put("USD/EUR", new BigDecimal("0.85"));
        baseRates.put("USD/GBP", new BigDecimal("0.78"));
        baseRates.put("USD/JPY", new BigDecimal("110.25"));
        baseRates.put("USD/CHF", new BigDecimal("0.92"));
        baseRates.put("USD/CAD", new BigDecimal("1.25"));
        baseRates.put("USD/AUD", new BigDecimal("1.35"));
        baseRates.put("EUR/GBP", new BigDecimal("0.92"));
        baseRates.put("EUR/JPY", new BigDecimal("130.00"));
        baseRates.put("GBP/JPY", new BigDecimal("141.35"));

        logger.info("Initialized mock FX rates for {} currency pairs", baseRates.size());
        lastUpdate = Instant.now();
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

        String pair = baseCurrency + "/" + quoteCurrency;
        String inversePair = quoteCurrency + "/" + baseCurrency;

        BigDecimal rate = baseRates.get(pair);
        if (rate == null) {
            BigDecimal inverseRate = baseRates.get(inversePair);
            if (inverseRate != null) {
                rate = BigDecimal.ONE.divide(inverseRate, 8, RoundingMode.HALF_UP);
            }
        }

        if (rate == null) {
            return Optional.empty();
        }

        // Add some random fluctuation (±0.1%)
        double fluctuation = (Math.random() - 0.5) * 0.002;
        BigDecimal adjustedRate = rate.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(fluctuation)))
            .setScale(8, RoundingMode.HALF_UP);

        ExchangeRate exchangeRate = ExchangeRate.create(
            baseCurrency,
            quoteCurrency,
            adjustedRate,
            DEFAULT_SPREAD,
            PROVIDER_NAME,
            Instant.now(),
            Instant.now().plus(5, ChronoUnit.MINUTES)
        );

        logger.debug("Generated mock rate for {}: {}", pair, adjustedRate);
        return Optional.of(exchangeRate);
    }

    @Override
    public List<ExchangeRate> getAllRates() {
        if (!isEnabled()) {
            return List.of();
        }

        return baseRates.entrySet().stream()
            .map(entry -> {
                String[] currencies = entry.getKey().split("/");
                String baseCurrency = currencies[0];
                String quoteCurrency = currencies[1];
                BigDecimal rate = entry.getValue();

                return ExchangeRate.create(
                    baseCurrency,
                    quoteCurrency,
                    rate,
                    DEFAULT_SPREAD,
                    PROVIDER_NAME,
                    Instant.now(),
                    Instant.now().plus(5, ChronoUnit.MINUTES)
                );
            })
            .toList();
    }

    @Override
    public boolean supports(String baseCurrency, String quoteCurrency) {
        String pair = baseCurrency + "/" + quoteCurrency;
        String inversePair = quoteCurrency + "/" + baseCurrency;
        return baseRates.containsKey(pair) || baseRates.containsKey(inversePair);
    }

    @Override
    public Instant getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public ProviderStatus getStatus() {
        return status;
    }

    public void updateRate(String baseCurrency, String quoteCurrency, BigDecimal rate) {
        String pair = baseCurrency + "/" + quoteCurrency;
        baseRates.put(pair, rate);
        lastUpdate = Instant.now();
        logger.info("Updated mock rate for {}: {}", pair, rate);
    }

    public void setStatus(ProviderStatus status) {
        this.status = status;
        logger.info("Mock FX provider status changed to: {}", status);
    }
}