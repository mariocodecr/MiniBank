package com.minibank.fx.adapter.persistence;

import com.minibank.accounts.adapter.persistence.SupportedCurrencyEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "exchange_rates",
       indexes = {
           @Index(name = "idx_exchange_rates_pair", columnList = "base_currency_code, quote_currency_code"),
           @Index(name = "idx_exchange_rates_provider", columnList = "provider_code"),
           @Index(name = "idx_exchange_rates_timestamp", columnList = "timestamp"),
           @Index(name = "idx_exchange_rates_valid_until", columnList = "valid_until")
       })
public class ExchangeRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "base_currency_code", nullable = false, length = 3)
    private String baseCurrencyCode;

    @Column(name = "quote_currency_code", nullable = false, length = 3)
    private String quoteCurrencyCode;

    @Column(nullable = false, precision = 15, scale = 8)
    private BigDecimal rate;

    @Column(name = "provider_code", nullable = false, length = 20)
    private String providerCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_currency_code", referencedColumnName = "currency_code", insertable = false, updatable = false)
    private SupportedCurrencyEntity baseCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_currency_code", referencedColumnName = "currency_code", insertable = false, updatable = false)
    private SupportedCurrencyEntity quoteCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_code", referencedColumnName = "provider_code", insertable = false, updatable = false)
    private FXProviderEntity provider;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExchangeRateEntity() {}

    public ExchangeRateEntity(UUID id, String baseCurrencyCode, String quoteCurrencyCode, BigDecimal rate,
                             String providerCode, Instant timestamp, Instant validUntil) {
        this.id = id;
        this.baseCurrencyCode = baseCurrencyCode;
        this.quoteCurrencyCode = quoteCurrencyCode;
        this.rate = rate;
        this.providerCode = providerCode;
        this.timestamp = timestamp;
        this.validUntil = validUntil;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBaseCurrencyCode() {
        return baseCurrencyCode;
    }

    public void setBaseCurrencyCode(String baseCurrencyCode) {
        this.baseCurrencyCode = baseCurrencyCode;
    }

    public String getQuoteCurrencyCode() {
        return quoteCurrencyCode;
    }

    public void setQuoteCurrencyCode(String quoteCurrencyCode) {
        this.quoteCurrencyCode = quoteCurrencyCode;
    }

    public SupportedCurrencyEntity getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(SupportedCurrencyEntity baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public SupportedCurrencyEntity getQuoteCurrency() {
        return quoteCurrency;
    }

    public void setQuoteCurrency(SupportedCurrencyEntity quoteCurrency) {
        this.quoteCurrency = quoteCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
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

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeRateEntity that = (ExchangeRateEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("ExchangeRateEntity{id=%s, %s/%s=%s, provider=%s, valid=%s}",
                           id, baseCurrencyCode, quoteCurrencyCode, rate, providerCode, validUntil);
    }
}