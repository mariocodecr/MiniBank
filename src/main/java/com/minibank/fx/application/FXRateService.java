package com.minibank.fx.application;

import com.minibank.fx.domain.ExchangeRate;
import com.minibank.fx.domain.ExchangeRateRepository;
import com.minibank.fx.domain.FXRateProvider;
import com.minibank.fx.infrastructure.cache.FXRateCache;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class FXRateService {
    private static final Logger logger = LoggerFactory.getLogger(FXRateService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(2);

    private final List<FXRateProvider> providers;
    private final FXRateCache cache;
    private final ExchangeRateRepository repository;
    
    // Metrics
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter providerFallbacks;
    private final Timer rateRetrievalLatency;

    public FXRateService(List<FXRateProvider> providers, FXRateCache cache, 
                        ExchangeRateRepository repository, MeterRegistry meterRegistry) {
        this.providers = providers.stream()
            .sorted(Comparator.comparingInt(FXRateProvider::getPriority))
            .toList();
        this.cache = cache;
        this.repository = repository;
        
        // Initialize metrics
        this.cacheHits = Counter.builder("fx.rate.cache.hits")
            .description("Number of FX rate cache hits")
            .register(meterRegistry);
        this.cacheMisses = Counter.builder("fx.rate.cache.misses")
            .description("Number of FX rate cache misses")
            .register(meterRegistry);
        this.providerFallbacks = Counter.builder("fx.rate.provider.fallbacks")
            .description("Number of FX rate provider fallbacks")
            .register(meterRegistry);
        this.rateRetrievalLatency = Timer.builder("fx.rate.retrieval.duration")
            .description("FX rate retrieval latency")
            .register(meterRegistry);
        
        logger.info("Initialized FX Rate Service with {} providers", this.providers.size());
    }

    public Optional<ExchangeRate> getExchangeRate(String baseCurrency, String quoteCurrency) {
        Timer.Sample sample = Timer.start();
        
        try {
            if (baseCurrency.equals(quoteCurrency)) {
                return Optional.of(createUnityRate(baseCurrency));
            }

            // 1. Try cache first
            Optional<ExchangeRate> cachedRate = cache.get(baseCurrency, quoteCurrency);
            if (cachedRate.isPresent()) {
                cacheHits.increment();
                logger.debug("Cache hit for {}/{}", baseCurrency, quoteCurrency);
                return cachedRate;
            }
            
            cacheMisses.increment();
            logger.debug("Cache miss for {}/{}", baseCurrency, quoteCurrency);

            // 2. Try providers in priority order
            for (FXRateProvider provider : providers) {
                if (!provider.isEnabled() || !provider.supports(baseCurrency, quoteCurrency)) {
                    continue;
                }

                try {
                    Optional<ExchangeRate> rate = provider.getRate(baseCurrency, quoteCurrency);
                    if (rate.isPresent()) {
                        ExchangeRate exchangeRate = rate.get();
                        
                        // Cache and persist the rate
                        cache.put(baseCurrency, quoteCurrency, exchangeRate, CACHE_TTL);
                        repository.save(exchangeRate);
                        
                        logger.debug("Retrieved rate from provider {}: {}/{} = {}", 
                                   provider.getProviderName(), baseCurrency, quoteCurrency, exchangeRate.getRate());
                        return rate;
                    }
                } catch (Exception e) {
                    logger.warn("Provider {} failed for {}/{}: {}", 
                              provider.getProviderName(), baseCurrency, quoteCurrency, e.getMessage());
                    providerFallbacks.increment();
                }
            }

            // 3. Fallback to database for historical rate
            Optional<ExchangeRate> historicalRate = repository.findLatestRate(baseCurrency, quoteCurrency);
            if (historicalRate.isPresent()) {
                logger.info("Using historical rate for {}/{}", baseCurrency, quoteCurrency);
                return historicalRate;
            }

            logger.warn("No exchange rate found for {}/{}", baseCurrency, quoteCurrency);
            return Optional.empty();
            
        } finally {
            sample.stop(rateRetrievalLatency);
        }
    }

    public Optional<ExchangeRate> getExchangeRateWithSpread(String baseCurrency, String quoteCurrency, BigDecimal customSpread) {
        Optional<ExchangeRate> rate = getExchangeRate(baseCurrency, quoteCurrency);
        return rate.map(r -> r.withSpread(customSpread));
    }

    public List<ExchangeRate> getAllActiveRates() {
        return providers.stream()
            .filter(FXRateProvider::isEnabled)
            .flatMap(provider -> {
                try {
                    return provider.getAllRates().stream();
                } catch (Exception e) {
                    logger.warn("Provider {} failed to return all rates: {}", provider.getProviderName(), e.getMessage());
                    return List.<ExchangeRate>of().stream();
                }
            })
            .toList();
    }

    public void refreshRates(String baseCurrency, String quoteCurrency) {
        logger.info("Manually refreshing rate for {}/{}", baseCurrency, quoteCurrency);
        
        // Clear cache to force refresh
        cache.remove(baseCurrency, quoteCurrency);
        
        // Fetch fresh rate
        getExchangeRate(baseCurrency, quoteCurrency);
    }

    public void refreshAllRates() {
        logger.info("Manually refreshing all rates");
        
        cache.clear();
        getAllActiveRates(); // This will populate cache and database
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void scheduledRateUpdate() {
        logger.debug("Starting scheduled rate update");
        
        try {
            // Evict expired rates from cache
            cache.evictExpired();
            
            // Update commonly used currency pairs
            List<String[]> commonPairs = List.of(
                new String[]{"USD", "EUR"},
                new String[]{"USD", "GBP"},
                new String[]{"USD", "JPY"},
                new String[]{"EUR", "GBP"},
                new String[]{"EUR", "JPY"},
                new String[]{"GBP", "JPY"}
            );
            
            for (String[] pair : commonPairs) {
                try {
                    getExchangeRate(pair[0], pair[1]);
                } catch (Exception e) {
                    logger.warn("Failed to update rate for {}/{}: {}", pair[0], pair[1], e.getMessage());
                }
            }
            
            logger.debug("Completed scheduled rate update");
            
        } catch (Exception e) {
            logger.error("Error during scheduled rate update: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredRates() {
        try {
            Instant cutoff = Instant.now().minusSeconds(3600 * 24); // 24 hours ago
            repository.deleteExpiredRates(cutoff);
            logger.info("Cleaned up expired rates older than {}", cutoff);
        } catch (Exception e) {
            logger.error("Error during expired rate cleanup: {}", e.getMessage(), e);
        }
    }

    public List<FXRateProvider> getProviders() {
        return List.copyOf(providers);
    }

    public FXRateProvider.ProviderStatus getProviderStatus(String providerName) {
        return providers.stream()
            .filter(p -> p.getProviderName().equals(providerName))
            .findFirst()
            .map(FXRateProvider::getStatus)
            .orElse(FXRateProvider.ProviderStatus.INACTIVE);
    }

    private ExchangeRate createUnityRate(String currency) {
        return ExchangeRate.create(
            currency,
            currency,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            "SYSTEM",
            Instant.now(),
            Instant.now().plusSeconds(Long.MAX_VALUE) // Never expires
        );
    }
}