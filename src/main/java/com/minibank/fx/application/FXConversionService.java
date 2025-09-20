package com.minibank.fx.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minibank.fx.domain.ExchangeRate;
import com.minibank.fx.domain.FXConversion;
import com.minibank.fx.domain.FXConversionRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
@Transactional
public class FXConversionService {
    private static final Logger logger = LoggerFactory.getLogger(FXConversionService.class);

    private final FXRateService rateService;
    private final FXConversionRepository conversionRepository;
    
    // Metrics
    private final Counter conversionsProcessed;
    private final Counter conversionFailures;
    private final Timer conversionLatency;

    public FXConversionService(FXRateService rateService, FXConversionRepository conversionRepository,
                              MeterRegistry meterRegistry) {
        this.rateService = rateService;
        this.conversionRepository = conversionRepository;
        
        // Initialize metrics
        this.conversionsProcessed = Counter.builder("fx.conversions.processed.total")
            .description("Total number of FX conversions processed")
            .register(meterRegistry);
        this.conversionFailures = Counter.builder("fx.conversions.failures.total")
            .description("Total number of FX conversion failures")
            .register(meterRegistry);
        this.conversionLatency = Timer.builder("fx.conversion.duration")
            .description("FX conversion processing latency")
            .register(meterRegistry);
    }

    public Optional<FXConversion> convertAmount(UUID accountId, String fromCurrency, String toCurrency,
                                              long fromAmountMinor, String correlationId) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.info("Processing FX conversion: {} {} {} -> {} for account {}", 
                       fromAmountMinor, fromCurrency, toCurrency, accountId);

            if (fromAmountMinor <= 0) {
                throw new IllegalArgumentException("Conversion amount must be positive");
            }

            if (fromCurrency.equals(toCurrency)) {
                logger.debug("Same currency conversion requested, creating identity conversion");
                FXConversion identityConversion = createIdentityConversion(accountId, fromCurrency, 
                                                                         fromAmountMinor, correlationId);
                return Optional.of(conversionRepository.save(identityConversion));
            }

            Optional<ExchangeRate> rate = rateService.getExchangeRate(fromCurrency, toCurrency);
            if (rate.isEmpty()) {
                logger.warn("No exchange rate available for {}/{}", fromCurrency, toCurrency);
                conversionFailures.increment();
                return Optional.empty();
            }

            FXConversion conversion = FXConversion.create(accountId, fromCurrency, toCurrency,
                                                        fromAmountMinor, rate.get(), correlationId);
            FXConversion savedConversion = conversionRepository.save(conversion);
            
            conversionsProcessed.increment();
            logger.info("FX conversion completed: {} {} -> {} {} (rate: {})", 
                       fromAmountMinor, fromCurrency, conversion.getToAmountMinor(), toCurrency, 
                       rate.get().getSellRate());
            
            return Optional.of(savedConversion);
            
        } catch (Exception e) {
            logger.error("FX conversion failed for account {}: {}", accountId, e.getMessage(), e);
            conversionFailures.increment();
            return Optional.empty();
        } finally {
            sample.stop(conversionLatency);
        }
    }

    public Optional<Long> calculateConvertedAmount(String fromCurrency, String toCurrency, long fromAmountMinor) {
        if (fromAmountMinor <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (fromCurrency.equals(toCurrency)) {
            return Optional.of(fromAmountMinor);
        }

        Optional<ExchangeRate> rate = rateService.getExchangeRate(fromCurrency, toCurrency);
        if (rate.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal fromAmount = BigDecimal.valueOf(fromAmountMinor);
        BigDecimal convertedAmount = fromAmount.multiply(rate.get().getSellRate());
        
        return Optional.of(convertedAmount.longValue());
    }

    public Optional<BigDecimal> getConversionRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return Optional.of(BigDecimal.ONE);
        }

        return rateService.getExchangeRate(fromCurrency, toCurrency)
            .map(ExchangeRate::getSellRate);
    }

    public Optional<BigDecimal> getConversionRateWithCustomSpread(String fromCurrency, String toCurrency, BigDecimal spread) {
        if (fromCurrency.equals(toCurrency)) {
            return Optional.of(BigDecimal.ONE);
        }

        return rateService.getExchangeRateWithSpread(fromCurrency, toCurrency, spread)
            .map(ExchangeRate::getSellRate);
    }

    @Transactional(readOnly = true)
    public List<FXConversion> getConversionsForAccount(UUID accountId) {
        return conversionRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<FXConversion> getConversionsForAccountAndPair(UUID accountId, String fromCurrency, String toCurrency) {
        return conversionRepository.findByAccountIdAndCurrencyPair(accountId, fromCurrency, toCurrency);
    }

    @Transactional(readOnly = true)
    public List<FXConversion> getConversionsForCorrelation(String correlationId) {
        return conversionRepository.findByCorrelationId(correlationId);
    }

    @Transactional(readOnly = true)
    public Optional<FXConversion> getConversion(UUID conversionId) {
        return conversionRepository.findById(conversionId);
    }

    public boolean isConversionSupported(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return true;
        }
        
        return rateService.getExchangeRate(fromCurrency, toCurrency).isPresent();
    }

    private FXConversion createIdentityConversion(UUID accountId, String currency, long amount, String correlationId) {
        return FXConversion.fromEntity(
            UUID.randomUUID(),
            accountId,
            currency,
            currency,
            amount,
            amount,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            "SYSTEM",
            java.time.Instant.now(),
            correlationId
        );
    }
}