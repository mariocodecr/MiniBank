package com.minibank.treasury.domain;

import com.minibank.accounts.domain.Currency;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ExposureAlert {
    private final UUID id;
    private final Currency currency;
    private final AlertType alertType;
    private final AlertSeverity severity;
    private final BigDecimal currentExposure;
    private final BigDecimal threshold;
    private final String message;
    private final AlertStatus status;
    private final Instant triggeredAt;
    private final Instant acknowledgedAt;
    private final String acknowledgedBy;
    private final String resolutionNotes;

    public ExposureAlert(UUID id, Currency currency, AlertType alertType, AlertSeverity severity,
                        BigDecimal currentExposure, BigDecimal threshold, String message,
                        AlertStatus status, Instant triggeredAt, Instant acknowledgedAt,
                        String acknowledgedBy, String resolutionNotes) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        this.alertType = Objects.requireNonNull(alertType, "Alert type cannot be null");
        this.severity = Objects.requireNonNull(severity, "Severity cannot be null");
        this.currentExposure = Objects.requireNonNull(currentExposure, "Current exposure cannot be null");
        this.threshold = Objects.requireNonNull(threshold, "Threshold cannot be null");
        this.message = Objects.requireNonNull(message, "Message cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.triggeredAt = Objects.requireNonNull(triggeredAt, "Triggered at cannot be null");
        this.acknowledgedAt = acknowledgedAt;
        this.acknowledgedBy = acknowledgedBy;
        this.resolutionNotes = resolutionNotes;
    }

    public static ExposureAlert createThresholdAlert(CurrencyExposure exposure) {
        AlertSeverity severity = determineSeverity(exposure);
        String message = buildThresholdMessage(exposure);
        
        return new ExposureAlert(
            UUID.randomUUID(),
            exposure.getCurrency(),
            AlertType.THRESHOLD_BREACH,
            severity,
            exposure.getNetExposure(),
            exposure.getExposureThreshold(),
            message,
            AlertStatus.ACTIVE,
            Instant.now(),
            null,
            null,
            null
        );
    }

    public static ExposureAlert createLiquidityAlert(Currency currency, BigDecimal currentLiquidity, 
                                                   BigDecimal minimumThreshold) {
        String message = String.format("Low liquidity alert for %s: current=%s, minimum=%s",
                                      currency.getCode(), currentLiquidity, minimumThreshold);
        
        return new ExposureAlert(
            UUID.randomUUID(),
            currency,
            AlertType.LIQUIDITY_LOW,
            AlertSeverity.HIGH,
            currentLiquidity,
            minimumThreshold,
            message,
            AlertStatus.ACTIVE,
            Instant.now(),
            null,
            null,
            null
        );
    }

    public static ExposureAlert createVolatilityAlert(Currency currency, BigDecimal currentVolatility,
                                                     BigDecimal volatilityThreshold) {
        String message = String.format("High volatility alert for %s: current=%s%%, threshold=%s%%",
                                      currency.getCode(), 
                                      currentVolatility.multiply(new BigDecimal("100")),
                                      volatilityThreshold.multiply(new BigDecimal("100")));
        
        return new ExposureAlert(
            UUID.randomUUID(),
            currency,
            AlertType.VOLATILITY_HIGH,
            AlertSeverity.MEDIUM,
            currentVolatility,
            volatilityThreshold,
            message,
            AlertStatus.ACTIVE,
            Instant.now(),
            null,
            null,
            null
        );
    }

    public ExposureAlert acknowledge(String acknowledgedBy, String notes) {
        if (status != AlertStatus.ACTIVE) {
            throw new IllegalStateException("Only active alerts can be acknowledged");
        }
        
        return new ExposureAlert(
            id, currency, alertType, severity, currentExposure, threshold, message,
            AlertStatus.ACKNOWLEDGED, triggeredAt, Instant.now(), acknowledgedBy, notes
        );
    }

    public ExposureAlert resolve(String resolvedBy, String resolutionNotes) {
        if (status == AlertStatus.RESOLVED) {
            throw new IllegalStateException("Alert is already resolved");
        }
        
        return new ExposureAlert(
            id, currency, alertType, severity, currentExposure, threshold, message,
            AlertStatus.RESOLVED, triggeredAt, 
            acknowledgedAt != null ? acknowledgedAt : Instant.now(),
            acknowledgedBy != null ? acknowledgedBy : resolvedBy,
            resolutionNotes
        );
    }

    public boolean isActive() {
        return status == AlertStatus.ACTIVE;
    }

    public boolean requiresImmediateAction() {
        return isActive() && severity == AlertSeverity.CRITICAL;
    }

    public boolean isOverdue() {
        if (!isActive()) {
            return false;
        }
        
        // Alert is overdue if not acknowledged within timeframe based on severity
        Instant now = Instant.now();
        Duration overdueThreshold = switch (severity) {
            case CRITICAL -> Duration.ofMinutes(15);
            case HIGH -> Duration.ofMinutes(30);
            case MEDIUM -> Duration.ofMinutes(60);
            case LOW -> Duration.ofMinutes(120);
        };
        
        return now.isAfter(triggeredAt.plus((java.time.temporal.TemporalAmount) overdueThreshold));
    }

    private static AlertSeverity determineSeverity(CurrencyExposure exposure) {
        CurrencyExposure.ExposureRisk risk = exposure.getExposureRisk();
        return switch (risk) {
            case HIGH -> AlertSeverity.CRITICAL;
            case MEDIUM -> AlertSeverity.HIGH;
            case LOW -> AlertSeverity.MEDIUM;
        };
    }

    private static String buildThresholdMessage(CurrencyExposure exposure) {
        String position = exposure.isLongPosition() ? "long" : "short";
        return String.format("Exposure threshold breached for %s: %s position of %s exceeds threshold of %s",
                           exposure.getCurrency().getCode(),
                           position,
                           exposure.getNetExposure().abs(),
                           exposure.getExposureThreshold().abs());
    }

    // Getters
    public UUID getId() { return id; }
    public Currency getCurrency() { return currency; }
    public AlertType getAlertType() { return alertType; }
    public AlertSeverity getSeverity() { return severity; }
    public BigDecimal getCurrentExposure() { return currentExposure; }
    public BigDecimal getThreshold() { return threshold; }
    public String getMessage() { return message; }
    public AlertStatus getStatus() { return status; }
    public Instant getTriggeredAt() { return triggeredAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public String getAcknowledgedBy() { return acknowledgedBy; }
    public String getResolutionNotes() { return resolutionNotes; }

    public enum AlertType {
        THRESHOLD_BREACH,
        LIQUIDITY_LOW,
        VOLATILITY_HIGH,
        CONCENTRATION_RISK,
        HEDGE_EFFECTIVENESS
    }

    public enum AlertSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum AlertStatus {
        ACTIVE,
        ACKNOWLEDGED,
        RESOLVED
    }

    // Helper class for duration calculations
    private static class Duration {
        private final long seconds;
        
        private Duration(long seconds) {
            this.seconds = seconds;
        }
        
        static Duration ofMinutes(long minutes) {
            return new Duration(minutes * 60);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExposureAlert that = (ExposureAlert) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("ExposureAlert{currency=%s, type=%s, severity=%s, status=%s}",
                           currency.getCode(), alertType, severity, status);
    }
}