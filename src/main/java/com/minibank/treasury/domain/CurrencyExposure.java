package com.minibank.treasury.domain;

import com.minibank.accounts.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class CurrencyExposure {
    private final UUID id;
    private final Currency currency;
    private final BigDecimal totalAssets;
    private final BigDecimal totalLiabilities;
    private final BigDecimal netExposure;
    private final BigDecimal exposureThreshold;
    private final ExposureStatus status;
    private final Instant calculatedAt;
    private final Instant updatedAt;

    public CurrencyExposure(UUID id, Currency currency, BigDecimal totalAssets, 
                          BigDecimal totalLiabilities, BigDecimal exposureThreshold,
                          ExposureStatus status, Instant calculatedAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        this.totalAssets = Objects.requireNonNull(totalAssets, "Total assets cannot be null");
        this.totalLiabilities = Objects.requireNonNull(totalLiabilities, "Total liabilities cannot be null");
        this.netExposure = totalAssets.subtract(totalLiabilities);
        this.exposureThreshold = Objects.requireNonNull(exposureThreshold, "Exposure threshold cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.calculatedAt = Objects.requireNonNull(calculatedAt, "Calculated at cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
    }

    public static CurrencyExposure create(Currency currency, BigDecimal totalAssets, 
                                        BigDecimal totalLiabilities, BigDecimal exposureThreshold) {
        Instant now = Instant.now();
        BigDecimal netExposure = totalAssets.subtract(totalLiabilities);
        ExposureStatus status = determineStatus(netExposure, exposureThreshold);
        
        return new CurrencyExposure(
            UUID.randomUUID(),
            currency,
            totalAssets,
            totalLiabilities,
            exposureThreshold,
            status,
            now,
            now
        );
    }

    public CurrencyExposure updateExposure(BigDecimal newTotalAssets, BigDecimal newTotalLiabilities) {
        Instant now = Instant.now();
        ExposureStatus newStatus = determineStatus(
            newTotalAssets.subtract(newTotalLiabilities), 
            exposureThreshold
        );
        
        return new CurrencyExposure(
            id,
            currency,
            newTotalAssets,
            newTotalLiabilities,
            exposureThreshold,
            newStatus,
            calculatedAt,
            now
        );
    }

    public CurrencyExposure updateThreshold(BigDecimal newThreshold) {
        Instant now = Instant.now();
        ExposureStatus newStatus = determineStatus(netExposure, newThreshold);
        
        return new CurrencyExposure(
            id,
            currency,
            totalAssets,
            totalLiabilities,
            newThreshold,
            newStatus,
            calculatedAt,
            now
        );
    }

    public boolean isThresholdBreached() {
        return status == ExposureStatus.THRESHOLD_BREACHED;
    }

    public boolean isLongPosition() {
        return netExposure.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isShortPosition() {
        return netExposure.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getExposurePercentage() {
        if (totalAssets.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return netExposure.abs().divide(totalAssets, 4, BigDecimal.ROUND_HALF_UP);
    }

    public ExposureRisk getExposureRisk() {
        BigDecimal absExposure = netExposure.abs();
        BigDecimal threshold = exposureThreshold.abs();
        
        if (absExposure.compareTo(threshold.multiply(new BigDecimal("1.5"))) >= 0) {
            return ExposureRisk.HIGH;
        } else if (absExposure.compareTo(threshold) >= 0) {
            return ExposureRisk.MEDIUM;
        } else {
            return ExposureRisk.LOW;
        }
    }

    private static ExposureStatus determineStatus(BigDecimal netExposure, BigDecimal threshold) {
        if (netExposure.abs().compareTo(threshold.abs()) >= 0) {
            return ExposureStatus.THRESHOLD_BREACHED;
        }
        return ExposureStatus.WITHIN_LIMITS;
    }

    // Getters
    public UUID getId() { return id; }
    public Currency getCurrency() { return currency; }
    public BigDecimal getTotalAssets() { return totalAssets; }
    public BigDecimal getTotalLiabilities() { return totalLiabilities; }
    public BigDecimal getNetExposure() { return netExposure; }
    public BigDecimal getExposureThreshold() { return exposureThreshold; }
    public ExposureStatus getStatus() { return status; }
    public Instant getCalculatedAt() { return calculatedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public enum ExposureStatus {
        WITHIN_LIMITS,
        THRESHOLD_BREACHED
    }

    public enum ExposureRisk {
        LOW,
        MEDIUM,
        HIGH
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrencyExposure that = (CurrencyExposure) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("CurrencyExposure{currency=%s, netExposure=%s, status=%s, risk=%s}",
                           currency.getCode(), netExposure, status, getExposureRisk());
    }
}