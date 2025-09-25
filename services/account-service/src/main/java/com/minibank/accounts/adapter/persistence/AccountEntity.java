package com.minibank.accounts.adapter.persistence;

import com.minibank.accounts.domain.AccountStatus;
import com.minibank.accounts.domain.Currency;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
public class AccountEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;
    
    @Column(name = "balance_minor", nullable = false)
    private Long balanceMinor;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;
    
    @Version
    @Column(nullable = false)
    private Long version;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public AccountEntity() {}
    
    public AccountEntity(UUID id, UUID userId, Currency currency, Long balanceMinor, 
                        AccountStatus status, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.currency = currency;
        this.balanceMinor = balanceMinor;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
    
    public Long getBalanceMinor() {
        return balanceMinor;
    }
    
    public void setBalanceMinor(Long balanceMinor) {
        this.balanceMinor = balanceMinor;
    }
    
    public AccountStatus getStatus() {
        return status;
    }
    
    public void setStatus(AccountStatus status) {
        this.status = status;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}