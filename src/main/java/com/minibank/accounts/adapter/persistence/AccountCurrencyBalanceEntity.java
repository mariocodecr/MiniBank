package com.minibank.accounts.adapter.persistence;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_currency_balances")
@EntityListeners(AuditingEntityListener.class)
public class AccountCurrencyBalanceEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "available_amount_minor", nullable = false)
    private Long availableAmountMinor;
    
    @Column(name = "reserved_amount_minor", nullable = false)
    private Long reservedAmountMinor;
    
    @Column(name = "total_amount_minor", nullable = false)
    private Long totalAmountMinor;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    // Constructors
    public AccountCurrencyBalanceEntity() {
        this.id = UUID.randomUUID();
    }

    public AccountCurrencyBalanceEntity(UUID accountId, String currency, Long availableAmountMinor, 
                                      Long reservedAmountMinor, Long totalAmountMinor) {
        this();
        this.accountId = accountId;
        this.currency = currency;
        this.availableAmountMinor = availableAmountMinor;
        this.reservedAmountMinor = reservedAmountMinor;
        this.totalAmountMinor = totalAmountMinor;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Long getAvailableAmountMinor() { return availableAmountMinor; }
    public void setAvailableAmountMinor(Long availableAmountMinor) { 
        this.availableAmountMinor = availableAmountMinor;
        updateTotalAmount();
    }

    public Long getReservedAmountMinor() { return reservedAmountMinor; }
    public void setReservedAmountMinor(Long reservedAmountMinor) { 
        this.reservedAmountMinor = reservedAmountMinor;
        updateTotalAmount();
    }

    public Long getTotalAmountMinor() { return totalAmountMinor; }
    public void setTotalAmountMinor(Long totalAmountMinor) { this.totalAmountMinor = totalAmountMinor; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    private void updateTotalAmount() {
        if (availableAmountMinor != null && reservedAmountMinor != null) {
            this.totalAmountMinor = availableAmountMinor + reservedAmountMinor;
        }
    }
}