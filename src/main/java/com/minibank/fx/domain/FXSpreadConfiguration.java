package com.minibank.fx.domain;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;

public class FXSpreadConfiguration {
    private final String currencyPair;
    private final BigDecimal baseSpread;
    private final Map<VolumeType, BigDecimal> volumeBasedSpreads;
    private final Map<TimeType, BigDecimal> timeBasedAdjustments;
    private final BigDecimal maxSpread;
    private final boolean isActive;

    public FXSpreadConfiguration(String currencyPair, BigDecimal baseSpread,
                               Map<VolumeType, BigDecimal> volumeBasedSpreads,
                               Map<TimeType, BigDecimal> timeBasedAdjustments,
                               BigDecimal maxSpread, boolean isActive) {
        this.currencyPair = Objects.requireNonNull(currencyPair, "Currency pair cannot be null");
        this.baseSpread = Objects.requireNonNull(baseSpread, "Base spread cannot be null");
        this.volumeBasedSpreads = Map.copyOf(volumeBasedSpreads != null ? volumeBasedSpreads : Map.of());
        this.timeBasedAdjustments = Map.copyOf(timeBasedAdjustments != null ? timeBasedAdjustments : Map.of());
        this.maxSpread = Objects.requireNonNull(maxSpread, "Max spread cannot be null");
        this.isActive = isActive;
        
        validateConfiguration();
    }

    public static FXSpreadConfiguration defaultConfiguration(String currencyPair) {
        BigDecimal defaultSpread = new BigDecimal("0.0015"); // 15 basis points
        
        Map<VolumeType, BigDecimal> defaultVolumeSpreads = Map.of(
            VolumeType.SMALL, new BigDecimal("0.0020"),    // 20 basis points
            VolumeType.MEDIUM, new BigDecimal("0.0015"),   // 15 basis points
            VolumeType.LARGE, new BigDecimal("0.0010"),    // 10 basis points
            VolumeType.INSTITUTIONAL, new BigDecimal("0.0005") // 5 basis points
        );
        
        Map<TimeType, BigDecimal> defaultTimeAdjustments = Map.of(
            TimeType.MARKET_HOURS, BigDecimal.ZERO,
            TimeType.OFF_HOURS, new BigDecimal("0.0005"),     // +5 basis points
            TimeType.WEEKEND, new BigDecimal("0.0010")        // +10 basis points
        );
        
        return new FXSpreadConfiguration(
            currencyPair,
            defaultSpread,
            defaultVolumeSpreads,
            defaultTimeAdjustments,
            new BigDecimal("0.0050"), // 50 basis points max
            true
        );
    }

    public BigDecimal calculateSpread(BigDecimal transactionAmount, LocalTime currentTime, boolean isWeekend) {
        if (!isActive) {
            return baseSpread;
        }

        BigDecimal spread = baseSpread;
        
        // Apply volume-based adjustment
        VolumeType volumeType = determineVolumeType(transactionAmount);
        if (volumeBasedSpreads.containsKey(volumeType)) {
            spread = volumeBasedSpreads.get(volumeType);
        }
        
        // Apply time-based adjustment
        TimeType timeType = determineTimeType(currentTime, isWeekend);
        if (timeBasedAdjustments.containsKey(timeType)) {
            spread = spread.add(timeBasedAdjustments.get(timeType));
        }
        
        // Ensure spread doesn't exceed maximum
        return spread.min(maxSpread);
    }

    private VolumeType determineVolumeType(BigDecimal amount) {
        // Convert to USD equivalent for standardized thresholds
        if (amount.compareTo(new BigDecimal("1000000")) >= 0) {
            return VolumeType.INSTITUTIONAL;
        } else if (amount.compareTo(new BigDecimal("100000")) >= 0) {
            return VolumeType.LARGE;
        } else if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            return VolumeType.MEDIUM;
        } else {
            return VolumeType.SMALL;
        }
    }

    private TimeType determineTimeType(LocalTime currentTime, boolean isWeekend) {
        if (isWeekend) {
            return TimeType.WEEKEND;
        }
        
        // Market hours: 9 AM - 5 PM
        LocalTime marketOpen = LocalTime.of(9, 0);
        LocalTime marketClose = LocalTime.of(17, 0);
        
        if (currentTime.isAfter(marketOpen) && currentTime.isBefore(marketClose)) {
            return TimeType.MARKET_HOURS;
        } else {
            return TimeType.OFF_HOURS;
        }
    }

    private void validateConfiguration() {
        if (baseSpread.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base spread cannot be negative");
        }
        if (maxSpread.compareTo(baseSpread) < 0) {
            throw new IllegalArgumentException("Max spread cannot be less than base spread");
        }
        for (BigDecimal spread : volumeBasedSpreads.values()) {
            if (spread.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Volume-based spreads cannot be negative");
            }
        }
    }

    // Getters
    public String getCurrencyPair() { return currencyPair; }
    public BigDecimal getBaseSpread() { return baseSpread; }
    public Map<VolumeType, BigDecimal> getVolumeBasedSpreads() { return volumeBasedSpreads; }
    public Map<TimeType, BigDecimal> getTimeBasedAdjustments() { return timeBasedAdjustments; }
    public BigDecimal getMaxSpread() { return maxSpread; }
    public boolean isActive() { return isActive; }

    public enum VolumeType {
        SMALL,          // < $10,000
        MEDIUM,         // $10,000 - $100,000
        LARGE,          // $100,000 - $1,000,000
        INSTITUTIONAL   // > $1,000,000
    }

    public enum TimeType {
        MARKET_HOURS,   // 9 AM - 5 PM
        OFF_HOURS,      // Outside market hours
        WEEKEND         // Saturday/Sunday
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FXSpreadConfiguration that = (FXSpreadConfiguration) o;
        return Objects.equals(currencyPair, that.currencyPair);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currencyPair);
    }

    @Override
    public String toString() {
        return String.format("FXSpreadConfiguration{currencyPair='%s', baseSpread=%s, active=%s}",
                           currencyPair, baseSpread, isActive);
    }
}