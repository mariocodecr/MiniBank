package com.minibank.accounts.adapter.persistence;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_balances",
       uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "currency_code"}))
@EntityListeners(AuditingEntityListener.class)
public class AccountCurrencyBalanceEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "available_amount_minor", nullable = false)
    private Long availableAmountMinor;

    @Column(name = "reserved_amount_minor", nullable = false)
    private Long reservedAmountMinor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", referencedColumnName = "currency_code", insertable = false, updatable = false)
    private SupportedCurrencyEntity currency;
    
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

    public AccountCurrencyBalanceEntity(UUID accountId, String currencyCode, Long availableAmountMinor,
                                      Long reservedAmountMinor) {
        this();
        this.accountId = accountId;
        this.currencyCode = currencyCode;
        this.availableAmountMinor = availableAmountMinor;
        this.reservedAmountMinor = reservedAmountMinor;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public SupportedCurrencyEntity getCurrency() { return currency; }
    public void setCurrency(SupportedCurrencyEntity currency) { this.currency = currency; }

    public Long getAvailableAmountMinor() { return availableAmountMinor; }
    public void setAvailableAmountMinor(Long availableAmountMinor) {
        this.availableAmountMinor = availableAmountMinor;
    }

    public Long getReservedAmountMinor() { return reservedAmountMinor; }
    public void setReservedAmountMinor(Long reservedAmountMinor) {
        this.reservedAmountMinor = reservedAmountMinor;
    }

    public Long getTotalAmountMinor() {
        if (availableAmountMinor != null && reservedAmountMinor != null) {
            return availableAmountMinor + reservedAmountMinor;
        }
        return 0L;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}