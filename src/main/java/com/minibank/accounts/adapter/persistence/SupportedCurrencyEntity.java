package com.minibank.accounts.adapter.persistence;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "supported_currencies")
@EntityListeners(AuditingEntityListener.class)
public class SupportedCurrencyEntity {
    
    @Id
    @Column(name = "currency_code", length = 3)
    private String currencyCode;
    
    @Column(name = "currency_name", nullable = false, length = 100)
    private String currencyName;
    
    @Column(name = "decimal_places", nullable = false)
    private Integer decimalPlaces;
    
    @Column(name = "minimum_amount_minor", nullable = false)
    private Long minimumAmountMinor;
    
    @Column(name = "maximum_amount_minor")
    private Long maximumAmountMinor;
    
    @Column(name = "symbol", length = 10)
    private String symbol;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public SupportedCurrencyEntity() {}

    public SupportedCurrencyEntity(String currencyCode, String currencyName, Integer decimalPlaces, 
                                 Long minimumAmountMinor, String symbol, Boolean isActive) {
        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.decimalPlaces = decimalPlaces;
        this.minimumAmountMinor = minimumAmountMinor;
        this.symbol = symbol;
        this.isActive = isActive;
    }

    // Getters and Setters
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getCurrencyName() { return currencyName; }
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }

    public Integer getDecimalPlaces() { return decimalPlaces; }
    public void setDecimalPlaces(Integer decimalPlaces) { this.decimalPlaces = decimalPlaces; }

    public Long getMinimumAmountMinor() { return minimumAmountMinor; }
    public void setMinimumAmountMinor(Long minimumAmountMinor) { this.minimumAmountMinor = minimumAmountMinor; }

    public Long getMaximumAmountMinor() { return maximumAmountMinor; }
    public void setMaximumAmountMinor(Long maximumAmountMinor) { this.maximumAmountMinor = maximumAmountMinor; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}