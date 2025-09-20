package com.minibank.treasury.application;

import com.minibank.accounts.application.MultiCurrencyAccountService;
import com.minibank.accounts.domain.Currency;
import com.minibank.accounts.domain.CurrencyBalance;
import com.minibank.accounts.domain.MultiCurrencyAccount;
import com.minibank.fx.application.FXRateService;
import com.minibank.fx.domain.ExchangeRate;
import com.minibank.treasury.domain.CurrencyExposure;
import com.minibank.treasury.domain.ExposureAlert;
import com.minibank.treasury.infrastructure.events.TreasuryEventPublisher;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TreasuryRiskService {
    private static final Logger logger = LoggerFactory.getLogger(TreasuryRiskService.class);
    private static final BigDecimal DEFAULT_EXPOSURE_THRESHOLD = new BigDecimal("1000000"); // $1M USD equivalent

    private final MultiCurrencyAccountService accountService;
    private final FXRateService fxRateService;
    private final TreasuryEventPublisher eventPublisher;
    
    // In-memory storage for exposure calculations and alerts
    private final Map<Currency, CurrencyExposure> currentExposures = new ConcurrentHashMap<>();
    private final Map<Currency, BigDecimal> exposureThresholds = new ConcurrentHashMap<>();
    private final Map<UUID, ExposureAlert> activeAlerts = new ConcurrentHashMap<>();
    
    // Metrics
    private final Counter exposureCalculations;
    private final Counter alertsGenerated;
    private final Counter thresholdBreaches;
    private final Map<Currency, Gauge> exposureGauges = new ConcurrentHashMap<>();
    private final MeterRegistry registry;

    public TreasuryRiskService(MultiCurrencyAccountService accountService,
                              FXRateService fxRateService,
                              TreasuryEventPublisher eventPublisher,
                              MeterRegistry meterRegistry) {
        this.accountService = accountService;
        this.fxRateService = fxRateService;
        this.eventPublisher = eventPublisher;
        this.registry = meterRegistry;
        
        // Initialize metrics
        this.exposureCalculations = Counter.builder("treasury.exposure.calculations.total")
            .description("Total number of exposure calculations")
            .register(meterRegistry);
        this.alertsGenerated = Counter.builder("treasury.alerts.generated.total")
            .description("Total number of alerts generated")
            .register(meterRegistry);
        this.thresholdBreaches = Counter.builder("treasury.threshold.breaches.total")
            .description("Total number of threshold breaches")
            .register(meterRegistry);
        
        // Initialize default thresholds
        initializeDefaultThresholds();
    }

    public Map<Currency, CurrencyExposure> calculateAllExposures() {
        logger.info("Calculating currency exposures for all supported currencies");
        exposureCalculations.increment();
        
        Map<Currency, CurrencyExposure> exposures = new HashMap<>();
        
        // Get all supported currencies from accounts
        Set<Currency> supportedCurrencies = getAllSupportedCurrencies();
        
        for (Currency currency : supportedCurrencies) {
            try {
                CurrencyExposure exposure = calculateCurrencyExposure(currency);
                exposures.put(currency, exposure);
                
                // Update current exposures
                currentExposures.put(currency, exposure);
                
                // Update exposure gauge metric
                updateExposureGauge(currency, exposure);
                
                // Check for threshold breaches
                checkThresholdBreach(exposure);
                
            } catch (Exception e) {
                logger.error("Failed to calculate exposure for currency {}: {}", 
                           currency.getCode(), e.getMessage());
            }
        }
        
        logger.info("Calculated exposures for {} currencies", exposures.size());
        return exposures;
    }

    public CurrencyExposure calculateCurrencyExposure(Currency currency) {
        logger.debug("Calculating exposure for currency: {}", currency.getCode());
        
        // Get all accounts with this currency
        List<MultiCurrencyAccount> accounts = accountService.findAccountsWithCurrency(currency.getCode());
        
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        
        for (MultiCurrencyAccount account : accounts) {
            CurrencyBalance balance = account.getBalance(currency);
            
            // Assets are positive balances
            if (balance.getTotalAmountMinor() > 0) {
                totalAssets = totalAssets.add(balance.getTotalAmount());
            } else if (balance.getTotalAmountMinor() < 0) {
                // Liabilities are negative balances (converted to positive)
                totalLiabilities = totalLiabilities.add(balance.getTotalAmount().abs());
            }
        }
        
        BigDecimal threshold = exposureThresholds.getOrDefault(currency, DEFAULT_EXPOSURE_THRESHOLD);
        
        return CurrencyExposure.create(currency, totalAssets, totalLiabilities, threshold);
    }

    public List<ExposureAlert> checkAllThresholds() {
        logger.info("Checking exposure thresholds for all currencies");
        
        List<ExposureAlert> newAlerts = new ArrayList<>();
        Map<Currency, CurrencyExposure> exposures = calculateAllExposures();
        
        for (CurrencyExposure exposure : exposures.values()) {
            if (exposure.isThresholdBreached()) {
                ExposureAlert alert = createThresholdAlert(exposure);
                newAlerts.add(alert);
                activeAlerts.put(alert.getId(), alert);
                thresholdBreaches.increment();
                
                // Publish alert event
                eventPublisher.publishExposureAlert(alert);
                
                logger.warn("Threshold breach alert generated for {}: exposure={}, threshold={}",
                          exposure.getCurrency().getCode(),
                          exposure.getNetExposure(),
                          exposure.getExposureThreshold());
            }
        }
        
        alertsGenerated.increment(newAlerts.size());
        return newAlerts;
    }

    public void setExposureThreshold(Currency currency, BigDecimal threshold) {
        logger.info("Setting exposure threshold for {} to {}", currency.getCode(), threshold);
        
        exposureThresholds.put(currency, threshold);
        
        // Recalculate exposure with new threshold
        CurrencyExposure exposure = calculateCurrencyExposure(currency);
        currentExposures.put(currency, exposure);
        
        // Check if new threshold is breached
        checkThresholdBreach(exposure);
    }

    public Map<Currency, BigDecimal> getExposureThresholds() {
        return Map.copyOf(exposureThresholds);
    }

    public List<ExposureAlert> getActiveAlerts() {
        return activeAlerts.values().stream()
            .filter(ExposureAlert::isActive)
            .sorted(Comparator.comparing(ExposureAlert::getTriggeredAt).reversed())
            .collect(Collectors.toList());
    }

    public List<ExposureAlert> getCriticalAlerts() {
        return activeAlerts.values().stream()
            .filter(alert -> alert.isActive() && alert.getSeverity() == ExposureAlert.AlertSeverity.CRITICAL)
            .sorted(Comparator.comparing(ExposureAlert::getTriggeredAt).reversed())
            .collect(Collectors.toList());
    }

    public void acknowledgeAlert(UUID alertId, String acknowledgedBy, String notes) {
        ExposureAlert alert = activeAlerts.get(alertId);
        if (alert == null) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }
        
        ExposureAlert acknowledgedAlert = alert.acknowledge(acknowledgedBy, notes);
        activeAlerts.put(alertId, acknowledgedAlert);
        
        // Publish alert acknowledged event
        eventPublisher.publishAlertAcknowledged(acknowledgedAlert);
        
        logger.info("Alert {} acknowledged by {}", alertId, acknowledgedBy);
    }

    public void resolveAlert(UUID alertId, String resolvedBy, String resolutionNotes) {
        ExposureAlert alert = activeAlerts.get(alertId);
        if (alert == null) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }
        
        ExposureAlert resolvedAlert = alert.resolve(resolvedBy, resolutionNotes);
        activeAlerts.put(alertId, resolvedAlert);
        
        // Publish alert resolved event
        eventPublisher.publishAlertResolved(resolvedAlert);
        
        logger.info("Alert {} resolved by {}: {}", alertId, resolvedBy, resolutionNotes);
    }

    public BigDecimal calculateTotalExposureInUSD() {
        BigDecimal totalExposureUSD = BigDecimal.ZERO;
        
        for (CurrencyExposure exposure : currentExposures.values()) {
            BigDecimal exposureInUSD = convertToUSD(exposure.getNetExposure(), exposure.getCurrency());
            totalExposureUSD = totalExposureUSD.add(exposureInUSD);
        }
        
        return totalExposureUSD;
    }

    public Map<Currency, BigDecimal> getExposureBreakdownInUSD() {
        Map<Currency, BigDecimal> breakdown = new HashMap<>();
        
        for (CurrencyExposure exposure : currentExposures.values()) {
            BigDecimal exposureInUSD = convertToUSD(exposure.getNetExposure(), exposure.getCurrency());
            breakdown.put(exposure.getCurrency(), exposureInUSD);
        }
        
        return breakdown;
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void scheduledExposureMonitoring() {
        try {
            logger.debug("Starting scheduled exposure monitoring");
            checkAllThresholds();
            cleanupResolvedAlerts();
        } catch (Exception e) {
            logger.error("Error during scheduled exposure monitoring: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void scheduledExposureReporting() {
        try {
            logger.info("Generating hourly exposure report");
            
            Map<Currency, CurrencyExposure> exposures = calculateAllExposures();
            BigDecimal totalUSDExposure = calculateTotalExposureInUSD();
            
            eventPublisher.publishExposureReport(exposures, totalUSDExposure);
            
            logger.info("Total exposure across all currencies: {} USD", totalUSDExposure);
            
        } catch (Exception e) {
            logger.error("Error during scheduled exposure reporting: {}", e.getMessage(), e);
        }
    }

    private void checkThresholdBreach(CurrencyExposure exposure) {
        if (exposure.isThresholdBreached()) {
            // Check if we already have an active alert for this currency
            boolean hasActiveAlert = activeAlerts.values().stream()
                .anyMatch(alert -> alert.isActive() && 
                         alert.getCurrency().equals(exposure.getCurrency()) &&
                         alert.getAlertType() == ExposureAlert.AlertType.THRESHOLD_BREACH);
            
            if (!hasActiveAlert) {
                ExposureAlert alert = createThresholdAlert(exposure);
                activeAlerts.put(alert.getId(), alert);
                eventPublisher.publishExposureAlert(alert);
                thresholdBreaches.increment();
            }
        }
    }

    private ExposureAlert createThresholdAlert(CurrencyExposure exposure) {
        return ExposureAlert.createThresholdAlert(exposure);
    }

    private void updateExposureGauge(Currency currency, CurrencyExposure exposure) {
        exposureGauges.computeIfAbsent(currency, c -> 
            Gauge.builder("treasury.exposure.net", this, treasury -> getCurrentExposure(c))
                .description("Net currency exposure")
                .tag("currency", c.getCode())
                .register(registry)
        );
    }

    private Double getCurrentExposure(Currency currency) {
        CurrencyExposure exposure = currentExposures.get(currency);
        return exposure != null ? exposure.getNetExposure().doubleValue() : 0.0;
    }

    private BigDecimal convertToUSD(BigDecimal amount, Currency fromCurrency) {
        if ("USD".equals(fromCurrency.getCode())) {
            return amount;
        }
        
        Optional<ExchangeRate> rate = fxRateService.getExchangeRate(fromCurrency.getCode(), "USD");
        if (rate.isPresent()) {
            return amount.multiply(rate.get().getRate()).setScale(2, RoundingMode.HALF_UP);
        }
        
        logger.warn("No exchange rate found for {}/USD, using amount as-is", fromCurrency.getCode());
        return amount;
    }

    private Set<Currency> getAllSupportedCurrencies() {
        // This would typically come from a currency service or repository
        // For now, return a hardcoded set of major currencies
        return Set.of(
            Currency.valueOf("USD"),
            Currency.valueOf("EUR"),
            Currency.valueOf("GBP"),
            Currency.valueOf("JPY"),
            Currency.valueOf("CHF"),
            Currency.valueOf("CAD"),
            Currency.valueOf("AUD")
        );
    }

    private void initializeDefaultThresholds() {
        // Set default thresholds for major currencies (in currency units)
        exposureThresholds.put(Currency.valueOf("USD"), new BigDecimal("1000000"));   // $1M
        exposureThresholds.put(Currency.valueOf("EUR"), new BigDecimal("850000"));    // €850K
        exposureThresholds.put(Currency.valueOf("GBP"), new BigDecimal("750000"));    // £750K
        exposureThresholds.put(Currency.valueOf("JPY"), new BigDecimal("110000000")); // ¥110M
        exposureThresholds.put(Currency.valueOf("CHF"), new BigDecimal("900000"));    // CHF 900K
        exposureThresholds.put(Currency.valueOf("CAD"), new BigDecimal("1300000"));   // CAD $1.3M
        exposureThresholds.put(Currency.valueOf("AUD"), new BigDecimal("1400000"));   // AUD $1.4M
        
        logger.info("Initialized default exposure thresholds for {} currencies", exposureThresholds.size());
    }

    private void cleanupResolvedAlerts() {
        // Remove alerts that were resolved more than 24 hours ago
        Instant cutoff = Instant.now().minusSeconds(24 * 3600);
        
        activeAlerts.entrySet().removeIf(entry -> {
            ExposureAlert alert = entry.getValue();
            return alert.getStatus() == ExposureAlert.AlertStatus.RESOLVED &&
                   alert.getAcknowledgedAt() != null &&
                   alert.getAcknowledgedAt().isBefore(cutoff);
        });
    }
}