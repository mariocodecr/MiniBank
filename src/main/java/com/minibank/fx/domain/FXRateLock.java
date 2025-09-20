package com.minibank.fx.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public class FXRateLock {
    private final UUID id;
    private final String baseCurrency;
    private final String quoteCurrency;
    private final BigDecimal lockedRate;
    private final BigDecimal spread;
    private final String provider;
    private final UUID accountId;
    private final String correlationId;
    private final Instant lockedAt;
    private final Instant expiresAt;
    private final FXRateLockStatus status;

    private FXRateLock(UUID id, String baseCurrency, String quoteCurrency, BigDecimal lockedRate,
                       BigDecimal spread, String provider, UUID accountId, String correlationId,
                       Instant lockedAt, Instant expiresAt, FXRateLockStatus status) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.baseCurrency = Objects.requireNonNull(baseCurrency, "Base currency cannot be null");
        this.quoteCurrency = Objects.requireNonNull(quoteCurrency, "Quote currency cannot be null");
        this.lockedRate = Objects.requireNonNull(lockedRate, "Locked rate cannot be null");
        this.spread = Objects.requireNonNull(spread, "Spread cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        this.accountId = Objects.requireNonNull(accountId, "Account ID cannot be null");
        this.correlationId = Objects.requireNonNull(correlationId, "Correlation ID cannot be null");
        this.lockedAt = Objects.requireNonNull(lockedAt, "Locked at cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expires at cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");

        if (lockedRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Locked rate must be positive");
        }
        if (spread.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Spread cannot be negative");
        }
        if (baseCurrency.equals(quoteCurrency)) {
            throw new IllegalArgumentException("Base and quote currencies cannot be the same");
        }
        if (expiresAt.isBefore(lockedAt)) {
            throw new IllegalArgumentException("Expiry time cannot be before lock time");
        }
    }

    public static FXRateLock create(String baseCurrency, String quoteCurrency, ExchangeRate exchangeRate,
                                   UUID accountId, String correlationId, int lockDurationMinutes) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lockDurationMinutes, ChronoUnit.MINUTES);

        return new FXRateLock(
            UUID.randomUUID(),
            baseCurrency,
            quoteCurrency,
            exchangeRate.getRate(),
            exchangeRate.getSpread(),
            exchangeRate.getProvider(),
            accountId,
            correlationId,
            now,
            expiresAt,
            FXRateLockStatus.ACTIVE
        );
    }

    public static FXRateLock fromEntity(UUID id, String baseCurrency, String quoteCurrency,
                                       BigDecimal lockedRate, BigDecimal spread, String provider,
                                       UUID accountId, String correlationId, Instant lockedAt,
                                       Instant expiresAt, FXRateLockStatus status) {
        return new FXRateLock(id, baseCurrency, quoteCurrency, lockedRate, spread, provider,
                             accountId, correlationId, lockedAt, expiresAt, status);
    }

    public FXRateLock use() {
        if (status != FXRateLockStatus.ACTIVE) {
            throw new IllegalStateException("Cannot use rate lock with status: " + status);
        }
        if (isExpired()) {
            throw new IllegalStateException("Cannot use expired rate lock");
        }

        return new FXRateLock(id, baseCurrency, quoteCurrency, lockedRate, spread, provider,
                             accountId, correlationId, lockedAt, expiresAt, FXRateLockStatus.USED);
    }

    public FXRateLock expire() {
        if (status == FXRateLockStatus.USED) {
            throw new IllegalStateException("Cannot expire used rate lock");
        }

        return new FXRateLock(id, baseCurrency, quoteCurrency, lockedRate, spread, provider,
                             accountId, correlationId, lockedAt, expiresAt, FXRateLockStatus.EXPIRED);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == FXRateLockStatus.ACTIVE && !isExpired();
    }

    public boolean isUsable() {
        return status == FXRateLockStatus.ACTIVE && !isExpired();
    }

    public String getCurrencyPair() {
        return baseCurrency + "/" + quoteCurrency;
    }

    public BigDecimal getEffectiveRate() {
        return lockedRate.subtract(spread);
    }

    public long calculateConvertedAmount(long fromAmountMinor) {
        if (fromAmountMinor <= 0) {
            throw new IllegalArgumentException("From amount must be positive");
        }

        BigDecimal fromAmount = BigDecimal.valueOf(fromAmountMinor);
        BigDecimal convertedAmount = fromAmount.multiply(getEffectiveRate());
        return convertedAmount.longValue();
    }

    public UUID getId() {
        return id;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public BigDecimal getLockedRate() {
        return lockedRate;
    }

    public BigDecimal getSpread() {
        return spread;
    }

    public String getProvider() {
        return provider;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public FXRateLockStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FXRateLock that = (FXRateLock) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("FXRateLock{id=%s, %s=%s, account=%s, status=%s, expires=%s}",
                           id, getCurrencyPair(), lockedRate, accountId, status, expiresAt);
    }

    public enum FXRateLockStatus {
        ACTIVE,   // Lock is active and can be used
        USED,     // Lock has been used in a transaction
        EXPIRED   // Lock has expired without being used
    }
}