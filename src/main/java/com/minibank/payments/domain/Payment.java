package com.minibank.payments.domain;

import com.minibank.accounts.domain.Currency;

import java.time.LocalDateTime;
import java.util.UUID;

public class Payment {
    private UUID id;
    private String requestId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private long amountMinor;
    private Currency currency;
    private PaymentStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    protected Payment() {}

    public Payment(UUID id, String requestId, UUID fromAccountId, UUID toAccountId,
                   long amountMinor, Currency currency, PaymentStatus status,
                   String failureReason, LocalDateTime createdAt, LocalDateTime updatedAt, Long version) {
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
        this.version = version;
    }

    public static Payment create(String requestId, UUID fromAccountId, UUID toAccountId,
                                long amountMinor, Currency currency) {
        validatePaymentRequest(requestId, fromAccountId, toAccountId, amountMinor);
        
        return new Payment(
            UUID.randomUUID(),
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            currency,
            PaymentStatus.REQUESTED,
            null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            0L
        );
    }

    public void markAsDebited() {
        validateStatusTransition(PaymentStatus.DEBITED);
        this.status = PaymentStatus.DEBITED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCredited() {
        validateStatusTransition(PaymentStatus.CREDITED);
        this.status = PaymentStatus.CREDITED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCompleted() {
        validateStatusTransition(PaymentStatus.COMPLETED);
        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed(PaymentStatus failureStatus, String reason) {
        if (!isFailureStatus(failureStatus)) {
            throw new IllegalArgumentException("Invalid failure status: " + failureStatus);
        }
        this.status = failureStatus;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCompensated() {
        validateStatusTransition(PaymentStatus.COMPENSATED);
        this.status = PaymentStatus.COMPENSATED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canBeDebited() {
        return status == PaymentStatus.REQUESTED;
    }

    public boolean canBeCredited() {
        return status == PaymentStatus.DEBITED;
    }

    public boolean canBeCompleted() {
        return status == PaymentStatus.CREDITED;
    }

    public boolean requiresCompensation() {
        return status == PaymentStatus.DEBITED && isFailureStatus(status);
    }

    public boolean isFinalStatus() {
        return status == PaymentStatus.COMPLETED || 
               status == PaymentStatus.COMPENSATED ||
               isFailureStatus(status);
    }

    private static void validatePaymentRequest(String requestId, UUID fromAccountId, 
                                              UUID toAccountId, long amountMinor) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        if (fromAccountId == null) {
            throw new IllegalArgumentException("From account ID cannot be null");
        }
        if (toAccountId == null) {
            throw new IllegalArgumentException("To account ID cannot be null");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void validateStatusTransition(PaymentStatus newStatus) {
        if (isFinalStatus()) {
            throw new IllegalStateException("Payment is in final status and cannot be updated");
        }
        
        switch (newStatus) {
            case DEBITED:
                if (status != PaymentStatus.REQUESTED) {
                    throw new IllegalStateException("Can only mark as DEBITED from REQUESTED status");
                }
                break;
            case CREDITED:
                if (status != PaymentStatus.DEBITED) {
                    throw new IllegalStateException("Can only mark as CREDITED from DEBITED status");
                }
                break;
            case COMPLETED:
                if (status != PaymentStatus.CREDITED) {
                    throw new IllegalStateException("Can only mark as COMPLETED from CREDITED status");
                }
                break;
            case COMPENSATED:
                if (status != PaymentStatus.DEBITED) {
                    throw new IllegalStateException("Can only compensate from DEBITED status");
                }
                break;
        }
    }

    private boolean isFailureStatus(PaymentStatus status) {
        return status == PaymentStatus.FAILED_INSUFFICIENT_FUNDS ||
               status == PaymentStatus.FAILED_ACCOUNT_INACTIVE ||
               status == PaymentStatus.FAILED_SYSTEM_ERROR;
    }

    // Getters
    public UUID getId() { return id; }
    public String getRequestId() { return requestId; }
    public UUID getFromAccountId() { return fromAccountId; }
    public UUID getToAccountId() { return toAccountId; }
    public long getAmountMinor() { return amountMinor; }
    public Currency getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}