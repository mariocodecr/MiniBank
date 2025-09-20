package com.minibank.compliance.application;

import com.minibank.compliance.domain.AMLScreeningResult;
import com.minibank.compliance.infrastructure.screening.AMLScreeningProvider;
import com.minibank.compliance.infrastructure.events.ComplianceEventPublisher;
import com.minibank.accounts.domain.MultiCurrencyAccount;
import com.minibank.accounts.application.MultiCurrencyAccountService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AMLScreeningService {
    private static final Logger logger = LoggerFactory.getLogger(AMLScreeningService.class);
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000"); // $10K USD
    private static final BigDecimal SUSPICIOUS_ROUND_AMOUNT_THRESHOLD = new BigDecimal("1000");

    private final List<AMLScreeningProvider> screeningProviders;
    private final MultiCurrencyAccountService accountService;
    private final ComplianceEventPublisher eventPublisher;
    
    // In-memory storage for screening results (in production, use a database)
    private final Map<UUID, AMLScreeningResult> screeningResults = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> lastScreeningTimes = new ConcurrentHashMap<>();
    
    // Metrics
    private final Counter screeningsPerformed;
    private final Counter screeningsPassed;
    private final Counter screeningsFailed;
    private final Counter screeningsRequiringReview;
    private final Timer screeningLatency;

    public AMLScreeningService(List<AMLScreeningProvider> screeningProviders,
                              MultiCurrencyAccountService accountService,
                              ComplianceEventPublisher eventPublisher,
                              MeterRegistry meterRegistry) {
        this.screeningProviders = screeningProviders.stream()
            .sorted(Comparator.comparingInt(AMLScreeningProvider::getPriority))
            .toList();
        this.accountService = accountService;
        this.eventPublisher = eventPublisher;
        
        // Initialize metrics
        this.screeningsPerformed = Counter.builder("compliance.aml.screenings.total")
            .description("Total number of AML screenings performed")
            .register(meterRegistry);
        this.screeningsPassed = Counter.builder("compliance.aml.screenings.passed")
            .description("Number of AML screenings that passed")
            .register(meterRegistry);
        this.screeningsFailed = Counter.builder("compliance.aml.screenings.failed")
            .description("Number of AML screenings that failed")
            .register(meterRegistry);
        this.screeningsRequiringReview = Counter.builder("compliance.aml.screenings.review_required")
            .description("Number of AML screenings requiring manual review")
            .register(meterRegistry);
        this.screeningLatency = Timer.builder("compliance.aml.screening.duration")
            .description("AML screening latency")
            .register(meterRegistry);
        
        logger.info("Initialized AML Screening Service with {} providers", this.screeningProviders.size());
    }

    public CompletableFuture<AMLScreeningResult> screenAccountOpening(UUID accountId) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start();
            
            try {
                logger.info("Performing AML screening for account opening: {}", accountId);
                
                MultiCurrencyAccount account = accountService.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
                
                // Perform account-level screening
                AMLScreeningResult result = performScreening(
                    accountId, 
                    null, 
                    AMLScreeningResult.ScreeningType.ACCOUNT_OPENING,
                    buildAccountScreeningData(account)
                );
                
                // Store result and update metrics
                screeningResults.put(result.getId(), result);
                lastScreeningTimes.put(accountId, result.getScreenedAt());
                updateMetrics(result);
                
                // Publish compliance event
                eventPublisher.publishAMLScreeningCompleted(result);
                
                logger.info("AML screening completed for account {}: {}", accountId, result.getStatus());
                return result;
                
            } finally {
                sample.stop(screeningLatency);
            }
        });
    }

    public CompletableFuture<AMLScreeningResult> screenCrossBorderPayment(UUID accountId, UUID transactionId, 
                                                                          String fromCurrency, String toCurrency,
                                                                          BigDecimal amount, String toCountry) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start();
            
            try {
                logger.info("Performing AML screening for cross-border payment: transaction={}, account={}", 
                          transactionId, accountId);
                
                MultiCurrencyAccount account = accountService.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
                
                // Build transaction screening data
                Map<String, Object> screeningData = buildTransactionScreeningData(
                    account, fromCurrency, toCurrency, amount, toCountry
                );
                
                // Perform transaction screening
                AMLScreeningResult result = performScreening(
                    accountId,
                    transactionId,
                    AMLScreeningResult.ScreeningType.CROSS_BORDER_PAYMENT,
                    screeningData
                );
                
                // Store result and update metrics
                screeningResults.put(result.getId(), result);
                updateMetrics(result);
                
                // Publish compliance event
                eventPublisher.publishAMLScreeningCompleted(result);
                
                logger.info("AML screening completed for transaction {}: {}", transactionId, result.getStatus());
                return result;
                
            } finally {
                sample.stop(screeningLatency);
            }
        });
    }

    public CompletableFuture<AMLScreeningResult> screenPeriodicReview(UUID accountId) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.start();
            
            try {
                logger.info("Performing periodic AML review for account: {}", accountId);
                
                // Check if account was screened recently
                Instant lastScreening = lastScreeningTimes.get(accountId);
                if (lastScreening != null && lastScreening.isAfter(Instant.now().minus(90, ChronoUnit.DAYS))) {
                    logger.debug("Account {} was screened recently, skipping periodic review", accountId);
                    return getLatestScreeningResult(accountId)
                        .orElseThrow(() -> new IllegalStateException("Latest screening result not found for account: " + accountId));
                }
                
                MultiCurrencyAccount account = accountService.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
                
                // Perform periodic review screening
                AMLScreeningResult result = performScreening(
                    accountId,
                    null,
                    AMLScreeningResult.ScreeningType.PERIODIC_REVIEW,
                    buildAccountScreeningData(account)
                );
                
                // Store result and update metrics
                screeningResults.put(result.getId(), result);
                lastScreeningTimes.put(accountId, result.getScreenedAt());
                updateMetrics(result);
                
                // Publish compliance event
                eventPublisher.publishAMLScreeningCompleted(result);
                
                logger.info("Periodic AML review completed for account {}: {}", accountId, result.getStatus());
                return result;
                
            } finally {
                sample.stop(screeningLatency);
            }
        });
    }

    public List<AMLScreeningResult> getScreeningResults(UUID accountId) {
        return screeningResults.values().stream()
            .filter(result -> result.getAccountId().equals(accountId))
            .sorted(Comparator.comparing(AMLScreeningResult::getScreenedAt).reversed())
            .toList();
    }

    public Optional<AMLScreeningResult> getLatestScreeningResult(UUID accountId) {
        return screeningResults.values().stream()
            .filter(result -> result.getAccountId().equals(accountId))
            .max(Comparator.comparing(AMLScreeningResult::getScreenedAt));
    }

    public List<AMLScreeningResult> getResultsRequiringReview() {
        return screeningResults.values().stream()
            .filter(AMLScreeningResult::requiresReview)
            .sorted(Comparator.comparing(AMLScreeningResult::getScreenedAt).reversed())
            .toList();
    }

    public List<AMLScreeningResult> getHighRiskResults() {
        return screeningResults.values().stream()
            .filter(AMLScreeningResult::hasHighRisk)
            .sorted(Comparator.comparing(AMLScreeningResult::getScreenedAt).reversed())
            .toList();
    }

    public boolean isAccountCompliant(UUID accountId) {
        Optional<AMLScreeningResult> latestResult = getLatestScreeningResult(accountId);
        if (latestResult.isEmpty()) {
            return false; // No screening performed
        }
        
        AMLScreeningResult result = latestResult.get();
        
        // Account is compliant if latest screening passed and was recent
        return result.isPassed() && 
               result.getScreenedAt().isAfter(Instant.now().minus(365, ChronoUnit.DAYS));
    }

    private AMLScreeningResult performScreening(UUID accountId, UUID transactionId, 
                                              AMLScreeningResult.ScreeningType screeningType,
                                              Map<String, Object> screeningData) {
        screeningsPerformed.increment();
        
        // Try each provider in priority order
        for (AMLScreeningProvider provider : screeningProviders) {
            if (!provider.isEnabled()) {
                continue;
            }
            
            try {
                AMLScreeningResult result = provider.performScreening(
                    accountId, transactionId, screeningType, screeningData
                );
                
                if (result != null) {
                    logger.debug("Screening completed by provider {}: {}", 
                               provider.getProviderName(), result.getStatus());
                    return result;
                }
                
            } catch (Exception e) {
                logger.warn("Screening provider {} failed: {}", provider.getProviderName(), e.getMessage());
            }
        }
        
        // If all providers fail, create a default "requires review" result
        logger.error("All AML screening providers failed for account {}", accountId);
        return AMLScreeningResult.createReviewRequired(
            accountId,
            transactionId,
            screeningType,
            "FALLBACK",
            50.0,
            List.of(new AMLScreeningResult.RiskIndicator(
                "PROVIDER_FAILURE",
                "All screening providers failed",
                8,
                "Technical failure in screening infrastructure"
            )),
            "All screening providers failed - manual review required"
        );
    }

    private Map<String, Object> buildAccountScreeningData(MultiCurrencyAccount account) {
        Map<String, Object> data = new HashMap<>();
        data.put("accountId", account.getId());
        data.put("accountHolderName", account.getAccountHolderName());
        data.put("email", account.getEmail());
        data.put("accountNumber", account.getAccountNumber());
        data.put("status", account.getStatus());
        data.put("supportedCurrencies", account.getSupportedCurrencies());
        data.put("createdAt", account.getCreatedAt());
        
        // Add balance information
        Map<String, BigDecimal> balances = new HashMap<>();
        account.getAllBalances().forEach((currency, balance) -> 
            balances.put(currency.getCode(), balance.getTotalAmount())
        );
        data.put("balances", balances);
        
        return data;
    }

    private Map<String, Object> buildTransactionScreeningData(MultiCurrencyAccount account,
                                                             String fromCurrency, String toCurrency,
                                                             BigDecimal amount, String toCountry) {
        Map<String, Object> data = buildAccountScreeningData(account);
        
        // Add transaction-specific data
        data.put("fromCurrency", fromCurrency);
        data.put("toCurrency", toCurrency);
        data.put("amount", amount);
        data.put("toCountry", toCountry);
        data.put("isHighValue", amount.compareTo(HIGH_VALUE_THRESHOLD) >= 0);
        data.put("isCrossBorder", !fromCurrency.equals(toCurrency));
        data.put("isRoundAmount", isRoundAmount(amount));
        
        // Add risk indicators based on transaction patterns
        List<String> riskFactors = new ArrayList<>();
        if (amount.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            riskFactors.add("HIGH_VALUE_TRANSACTION");
        }
        if (isRoundAmount(amount)) {
            riskFactors.add("ROUND_AMOUNT");
        }
        if (isHighRiskCountry(toCountry)) {
            riskFactors.add("HIGH_RISK_COUNTRY");
        }
        data.put("riskFactors", riskFactors);
        
        return data;
    }

    private boolean isRoundAmount(BigDecimal amount) {
        // Check if amount is a round number (multiple of 1000)
        return amount.remainder(SUSPICIOUS_ROUND_AMOUNT_THRESHOLD).compareTo(BigDecimal.ZERO) == 0;
    }

    private boolean isHighRiskCountry(String countryCode) {
        // List of high-risk countries for AML purposes
        Set<String> highRiskCountries = Set.of(
            "AF", "BY", "BA", "BF", "KH", "CF", "CD", "CU", "ET", "GH", "HT", "IR", "IQ",
            "LB", "LY", "ML", "MZ", "MM", "NI", "KP", "PK", "PS", "RU", "SO", "SS", "SD",
            "SY", "TZ", "TR", "UG", "UA", "VU", "YE", "ZW"
        );
        return highRiskCountries.contains(countryCode);
    }

    private void updateMetrics(AMLScreeningResult result) {
        switch (result.getStatus()) {
            case PASSED -> screeningsPassed.increment();
            case FAILED -> screeningsFailed.increment();
            case REQUIRES_REVIEW -> screeningsRequiringReview.increment();
        }
    }
}