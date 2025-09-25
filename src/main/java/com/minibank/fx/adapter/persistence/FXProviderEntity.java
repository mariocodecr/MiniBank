package com.minibank.fx.adapter.persistence;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "fx_providers")
@EntityListeners(AuditingEntityListener.class)
public class FXProviderEntity {

    @Id
    @Column(name = "provider_code", length = 20)
    private String providerCode;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "default_spread", nullable = false, precision = 15, scale = 8)
    private BigDecimal defaultSpread;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "priority_order", nullable = false)
    private Integer priorityOrder;

    @Column(name = "rate_validity_minutes", nullable = false)
    private Integer rateValidityMinutes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected FXProviderEntity() {}

    public FXProviderEntity(String providerCode, String providerName, BigDecimal defaultSpread,
                           Boolean isActive, Integer priorityOrder, Integer rateValidityMinutes) {
        this.providerCode = providerCode;
        this.providerName = providerName;
        this.defaultSpread = defaultSpread;
        this.isActive = isActive;
        this.priorityOrder = priorityOrder;
        this.rateValidityMinutes = rateValidityMinutes;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public BigDecimal getDefaultSpread() {
        return defaultSpread;
    }

    public void setDefaultSpread(BigDecimal defaultSpread) {
        this.defaultSpread = defaultSpread;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getPriorityOrder() {
        return priorityOrder;
    }

    public void setPriorityOrder(Integer priorityOrder) {
        this.priorityOrder = priorityOrder;
    }

    public Integer getRateValidityMinutes() {
        return rateValidityMinutes;
    }

    public void setRateValidityMinutes(Integer rateValidityMinutes) {
        this.rateValidityMinutes = rateValidityMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FXProviderEntity that = (FXProviderEntity) o;
        return Objects.equals(providerCode, that.providerCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerCode);
    }

    @Override
    public String toString() {
        return String.format("FXProviderEntity{code='%s', name='%s', active=%s}",
                           providerCode, providerName, isActive);
    }
}