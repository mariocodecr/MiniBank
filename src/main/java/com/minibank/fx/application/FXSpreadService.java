package com.minibank.fx.application;

import com.minibank.fx.domain.FXSpreadConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FXSpreadService {
    private static final Logger logger = LoggerFactory.getLogger(FXSpreadService.class);

    private final Map<String, FXSpreadConfiguration> spreadConfigurations = new ConcurrentHashMap<>();

    public FXSpreadService() {
        // Initialize with default configurations for major currency pairs
        initializeDefaultConfigurations();
    }

    @Cacheable("fx-spreads")
    public BigDecimal calculateSpread(String baseCurrency, String quoteCurrency, 
                                    BigDecimal transactionAmount) {
        return calculateSpread(baseCurrency, quoteCurrency, transactionAmount, LocalDateTime.now());
    }

    public BigDecimal calculateSpread(String baseCurrency, String quoteCurrency, 
                                    BigDecimal transactionAmount, LocalDateTime timestamp) {
        String currencyPair = normalizeCurrencyPair(baseCurrency, quoteCurrency);
        
        FXSpreadConfiguration config = getSpreadConfiguration(currencyPair);
        
        LocalTime currentTime = timestamp.toLocalTime();
        boolean isWeekend = timestamp.getDayOfWeek() == DayOfWeek.SATURDAY || 
                           timestamp.getDayOfWeek() == DayOfWeek.SUNDAY;
        
        BigDecimal spread = config.calculateSpread(transactionAmount, currentTime, isWeekend);
        
        logger.debug("Calculated spread for {}: {} (amount: {}, time: {}, weekend: {})", 
                   currencyPair, spread, transactionAmount, currentTime, isWeekend);
        
        return spread;
    }

    @Cacheable("fx-spread-configs")
    public FXSpreadConfiguration getSpreadConfiguration(String currencyPair) {
        return spreadConfigurations.computeIfAbsent(
            currencyPair, 
            pair -> FXSpreadConfiguration.defaultConfiguration(pair)
        );
    }

    @CacheEvict(value = {"fx-spreads", "fx-spread-configs"}, allEntries = true)
    public void updateSpreadConfiguration(FXSpreadConfiguration configuration) {
        logger.info("Updating spread configuration for: {}", configuration.getCurrencyPair());
        
        spreadConfigurations.put(configuration.getCurrencyPair(), configuration);
        
        logger.info("Updated spread configuration for {}: base={}, max={}", 
                   configuration.getCurrencyPair(), 
                   configuration.getBaseSpread(), 
                   configuration.getMaxSpread());
    }

    @CacheEvict(value = {"fx-spreads", "fx-spread-configs"}, allEntries = true)
    public void removeSpreadConfiguration(String currencyPair) {
        logger.info("Removing spread configuration for: {}", currencyPair);
        spreadConfigurations.remove(currencyPair);
    }

    public Map<String, FXSpreadConfiguration> getAllSpreadConfigurations() {
        return Map.copyOf(spreadConfigurations);
    }

    public Optional<FXSpreadConfiguration> findSpreadConfiguration(String currencyPair) {
        return Optional.ofNullable(spreadConfigurations.get(currencyPair));
    }

    public BigDecimal getEffectiveSpread(String baseCurrency, String quoteCurrency, 
                                       BigDecimal amount, LocalDateTime timestamp) {
        try {
            return calculateSpread(baseCurrency, quoteCurrency, amount, timestamp);
        } catch (Exception e) {
            logger.warn("Failed to calculate spread for {}/{}, using fallback: {}", 
                       baseCurrency, quoteCurrency, e.getMessage());
            return new BigDecimal("0.0015"); // 15 basis points fallback
        }
    }

    private void initializeDefaultConfigurations() {
        // Major currency pairs with tighter spreads
        String[] majorPairs = {
            "USD/EUR", "USD/GBP", "USD/JPY", "USD/CHF", 
            "EUR/GBP", "EUR/JPY", "GBP/JPY"
        };
        
        for (String pair : majorPairs) {
            spreadConfigurations.put(pair, createMajorPairConfiguration(pair));
        }
        
        // Minor currency pairs with wider spreads
        String[] minorPairs = {
            "USD/CAD", "USD/AUD", "USD/NZD", "USD/SEK", "USD/NOK", "USD/DKK",
            "EUR/CHF", "EUR/CAD", "EUR/AUD", "GBP/CHF", "GBP/CAD", "GBP/AUD"
        };
        
        for (String pair : minorPairs) {
            spreadConfigurations.put(pair, createMinorPairConfiguration(pair));
        }
        
        // Exotic currency pairs with wider spreads
        String[] exoticPairs = {
            "USD/CNY", "USD/INR", "USD/BRL", "USD/MXN", "USD/ZAR", "USD/TRY",
            "EUR/CNY", "EUR/INR", "GBP/CNY", "JPY/CNY"
        };
        
        for (String pair : exoticPairs) {
            spreadConfigurations.put(pair, createExoticPairConfiguration(pair));
        }
        
        logger.info("Initialized {} FX spread configurations", spreadConfigurations.size());
    }

    private FXSpreadConfiguration createMajorPairConfiguration(String currencyPair) {
        BigDecimal baseSpread = new BigDecimal("0.0008"); // 8 basis points
        
        Map<FXSpreadConfiguration.VolumeType, BigDecimal> volumeSpreads = Map.of(
            FXSpreadConfiguration.VolumeType.SMALL, new BigDecimal("0.0012"),
            FXSpreadConfiguration.VolumeType.MEDIUM, new BigDecimal("0.0008"),
            FXSpreadConfiguration.VolumeType.LARGE, new BigDecimal("0.0005"),
            FXSpreadConfiguration.VolumeType.INSTITUTIONAL, new BigDecimal("0.0003")
        );
        
        Map<FXSpreadConfiguration.TimeType, BigDecimal> timeAdjustments = Map.of(
            FXSpreadConfiguration.TimeType.MARKET_HOURS, BigDecimal.ZERO,
            FXSpreadConfiguration.TimeType.OFF_HOURS, new BigDecimal("0.0003"),
            FXSpreadConfiguration.TimeType.WEEKEND, new BigDecimal("0.0005")
        );
        
        return new FXSpreadConfiguration(
            currencyPair, baseSpread, volumeSpreads, timeAdjustments,
            new BigDecimal("0.0030"), true
        );
    }

    private FXSpreadConfiguration createMinorPairConfiguration(String currencyPair) {
        BigDecimal baseSpread = new BigDecimal("0.0015"); // 15 basis points
        
        Map<FXSpreadConfiguration.VolumeType, BigDecimal> volumeSpreads = Map.of(
            FXSpreadConfiguration.VolumeType.SMALL, new BigDecimal("0.0020"),
            FXSpreadConfiguration.VolumeType.MEDIUM, new BigDecimal("0.0015"),
            FXSpreadConfiguration.VolumeType.LARGE, new BigDecimal("0.0010"),
            FXSpreadConfiguration.VolumeType.INSTITUTIONAL, new BigDecimal("0.0008")
        );
        
        Map<FXSpreadConfiguration.TimeType, BigDecimal> timeAdjustments = Map.of(
            FXSpreadConfiguration.TimeType.MARKET_HOURS, BigDecimal.ZERO,
            FXSpreadConfiguration.TimeType.OFF_HOURS, new BigDecimal("0.0005"),
            FXSpreadConfiguration.TimeType.WEEKEND, new BigDecimal("0.0010")
        );
        
        return new FXSpreadConfiguration(
            currencyPair, baseSpread, volumeSpreads, timeAdjustments,
            new BigDecimal("0.0050"), true
        );
    }

    private FXSpreadConfiguration createExoticPairConfiguration(String currencyPair) {
        BigDecimal baseSpread = new BigDecimal("0.0030"); // 30 basis points
        
        Map<FXSpreadConfiguration.VolumeType, BigDecimal> volumeSpreads = Map.of(
            FXSpreadConfiguration.VolumeType.SMALL, new BigDecimal("0.0040"),
            FXSpreadConfiguration.VolumeType.MEDIUM, new BigDecimal("0.0030"),
            FXSpreadConfiguration.VolumeType.LARGE, new BigDecimal("0.0025"),
            FXSpreadConfiguration.VolumeType.INSTITUTIONAL, new BigDecimal("0.0020")
        );
        
        Map<FXSpreadConfiguration.TimeType, BigDecimal> timeAdjustments = Map.of(
            FXSpreadConfiguration.TimeType.MARKET_HOURS, BigDecimal.ZERO,
            FXSpreadConfiguration.TimeType.OFF_HOURS, new BigDecimal("0.0010"),
            FXSpreadConfiguration.TimeType.WEEKEND, new BigDecimal("0.0020")
        );
        
        return new FXSpreadConfiguration(
            currencyPair, baseSpread, volumeSpreads, timeAdjustments,
            new BigDecimal("0.0100"), true
        );
    }

    private String normalizeCurrencyPair(String baseCurrency, String quoteCurrency) {
        return baseCurrency + "/" + quoteCurrency;
    }
}