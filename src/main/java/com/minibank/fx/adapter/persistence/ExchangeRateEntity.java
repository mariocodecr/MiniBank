package com.minibank.fx.adapter.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "fx_rates", 
       indexes = {
           @Index(name = "idx_fx_rates_pair", columnList = "base_currency, quote_currency"),
           @Index(name = "idx_fx_rates_provider", columnList = "provider"),
           @Index(name = "idx_fx_rates_timestamp", columnList = "timestamp"),
           @Index(name = "idx_fx_rates_valid_until", columnList = "valid_until")
       })
public class ExchangeRateEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "quote_currency", nullable = false, length = 3)
    private String quoteCurrency;

    @Column(nullable = false, precision = 15, scale = 8)
    private BigDecimal rate;

    @Column(nullable = false, precision = 15, scale = 8)
    private BigDecimal spread;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExchangeRateEntity() {}

    public ExchangeRateEntity(UUID id, String baseCurrency, String quoteCurrency, BigDecimal rate,
                             BigDecimal spread, String provider, Instant timestamp, Instant validUntil) {
        this.id = id;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rate = rate;
        this.spread = spread;
        this.provider = provider;
        this.timestamp = timestamp;
        this.validUntil = validUntil;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public void setQuoteCurrency(String quoteCurrency) {
        this.quoteCurrency = quoteCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
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
                           id, baseCurrency, quoteCurrency, rate, provider, validUntil);
    }
}