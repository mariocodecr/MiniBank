package com.minibank.payments.adapter.web.dto;

import com.minibank.accounts.domain.Currency;
import com.minibank.payments.domain.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentResponse {
    private UUID id;
    private String requestId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private Long amountMinor;
    private Currency currency;
    private PaymentStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentResponse() {}

    public PaymentResponse(UUID id, String requestId, UUID fromAccountId, UUID toAccountId,
                          Long amountMinor, Currency currency, PaymentStatus status,
                          String failureReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.requestId = requestId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public UUID getId() { return id; }
    public String getRequestId() { return requestId; }
    public UUID getFromAccountId() { return fromAccountId; }
    public UUID getToAccountId() { return toAccountId; }
    public Long getAmountMinor() { return amountMinor; }
    public Currency getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}