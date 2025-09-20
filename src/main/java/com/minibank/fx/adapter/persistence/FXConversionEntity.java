package com.minibank.fx.adapter.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "fx_conversions",
       indexes = {
           @Index(name = "idx_fx_conversions_account", columnList = "account_id"),
           @Index(name = "idx_fx_conversions_pair", columnList = "from_currency, to_currency"),
           @Index(name = "idx_fx_conversions_correlation", columnList = "correlation_id"),
           @Index(name = "idx_fx_conversions_timestamp", columnList = "timestamp")
       })
public class FXConversionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(name = "from_amount_minor", nullable = false)
    private Long fromAmountMinor;

    @Column(name = "to_amount_minor", nullable = false)
    private Long toAmountMinor;

    @Column(name = "exchange_rate", nullable = false, precision = 15, scale = 8)
    private BigDecimal exchangeRate;

    @Column(nullable = false, precision = 15, scale = 8)
    private BigDecimal spread;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FXConversionEntity() {}

    public FXConversionEntity(UUID id, UUID accountId, String fromCurrency, String toCurrency,
                             Long fromAmountMinor, Long toAmountMinor, BigDecimal exchangeRate,
                             BigDecimal spread, String provider, Instant timestamp, String correlationId) {
        this.id = id;
        this.accountId = accountId;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.fromAmountMinor = fromAmountMinor;
        this.toAmountMinor = toAmountMinor;
        this.exchangeRate = exchangeRate;
        this.spread = spread;
        this.provider = provider;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(String fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(String toCurrency) {
        this.toCurrency = toCurrency;
    }

    public Long getFromAmountMinor() {
        return fromAmountMinor;
    }

    public void setFromAmountMinor(Long fromAmountMinor) {
        this.fromAmountMinor = fromAmountMinor;
    }

    public Long getToAmountMinor() {
        return toAmountMinor;
    }

    public void setToAmountMinor(Long toAmountMinor) {
        this.toAmountMinor = toAmountMinor;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public BigDecimal getSpread() {
        return spread;
    }

    public void setSpread(BigDecimal spread) {
        this.spread = spread;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FXConversionEntity that = (FXConversionEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("FXConversionEntity{id=%s, %s->%s, %d->%d, account=%s}", 
                           id, fromCurrency, toCurrency, fromAmountMinor, toAmountMinor, accountId);
    }
}