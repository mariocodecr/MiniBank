package com.minibank.fx.adapter.persistence;

import com.minibank.accounts.adapter.persistence.SupportedCurrencyEntity;
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
           @Index(name = "idx_fx_conversions_pair", columnList = "from_currency_code, to_currency_code"),
           @Index(name = "idx_fx_conversions_correlation", columnList = "correlation_id"),
           @Index(name = "idx_fx_conversions_timestamp", columnList = "timestamp")
       })
public class FXConversionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "from_currency_code", nullable = false, length = 3)
    private String fromCurrencyCode;

    @Column(name = "to_currency_code", nullable = false, length = 3)
    private String toCurrencyCode;

    @Column(name = "from_amount_minor", nullable = false)
    private Long fromAmountMinor;

    @Column(name = "to_amount_minor", nullable = false)
    private Long toAmountMinor;

    @Column(name = "exchange_rate", nullable = false, precision = 15, scale = 8)
    private BigDecimal exchangeRate;

    @Column(name = "provider_code", nullable = false, length = 20)
    private String providerCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_currency_code", referencedColumnName = "currency_code", insertable = false, updatable = false)
    private SupportedCurrencyEntity fromCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_currency_code", referencedColumnName = "currency_code", insertable = false, updatable = false)
    private SupportedCurrencyEntity toCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_code", referencedColumnName = "provider_code", insertable = false, updatable = false)
    private FXProviderEntity provider;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FXConversionEntity() {}

    public FXConversionEntity(UUID id, UUID accountId, String fromCurrencyCode, String toCurrencyCode,
                             Long fromAmountMinor, Long toAmountMinor, BigDecimal exchangeRate,
                             String providerCode, Instant timestamp, String correlationId) {
        this.id = id;
        this.accountId = accountId;
        this.fromCurrencyCode = fromCurrencyCode;
        this.toCurrencyCode = toCurrencyCode;
        this.fromAmountMinor = fromAmountMinor;
        this.toAmountMinor = toAmountMinor;
        this.exchangeRate = exchangeRate;
        this.providerCode = providerCode;
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

    public String getFromCurrencyCode() {
        return fromCurrencyCode;
    }

    public void setFromCurrencyCode(String fromCurrencyCode) {
        this.fromCurrencyCode = fromCurrencyCode;
    }

    public String getToCurrencyCode() {
        return toCurrencyCode;
    }

    public void setToCurrencyCode(String toCurrencyCode) {
        this.toCurrencyCode = toCurrencyCode;
    }

    public SupportedCurrencyEntity getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(SupportedCurrencyEntity fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public SupportedCurrencyEntity getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(SupportedCurrencyEntity toCurrency) {
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

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public FXProviderEntity getProvider() {
        return provider;
    }

    public void setProvider(FXProviderEntity provider) {
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
                           id, fromCurrencyCode, toCurrencyCode, fromAmountMinor, toAmountMinor, accountId);
    }
}