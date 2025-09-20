package com.minibank.payments.domain.events;

import com.minibank.accounts.domain.Currency;

import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentDomainEvent {
    private final String eventId;
    private final UUID paymentId;
    private final PaymentEventType eventType;
    private final LocalDateTime timestamp;
    private final String correlationId;
    private final String requestId;
    private final UUID fromAccountId;
    private final UUID toAccountId;
    private final long amountMinor;
    private final Currency currency;
    private final String failureReason;
    private final boolean compensationRequired;

    public PaymentDomainEvent(String eventId, UUID paymentId, PaymentEventType eventType,
                             LocalDateTime timestamp, String correlationId, String requestId,
                             UUID fromAccountId, UUID toAccountId, long amountMinor, Currency currency,
                             String failureReason, boolean compensationRequired) {
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
        this.requestId = requestId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.failureReason = failureReason;
        this.compensationRequired = compensationRequired;
    }

    public static PaymentDomainEvent paymentInitiated(UUID paymentId, UUID fromAccountId, UUID toAccountId, 
                                                     long amountMinor, String currency, String requestId, String description) {
        return new PaymentDomainEvent(
            UUID.randomUUID().toString(),
            paymentId,
            PaymentEventType.PAYMENT_REQUESTED,
            LocalDateTime.now(),
            requestId,
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            Currency.valueOf(currency),
            null,
            false
        );
    }

    public static PaymentDomainEvent paymentRequested(UUID paymentId, String correlationId, String requestId,
                                                     UUID fromAccountId, UUID toAccountId, long amountMinor, Currency currency) {
        return new PaymentDomainEvent(
            UUID.randomUUID().toString(),
            paymentId,
            PaymentEventType.PAYMENT_REQUESTED,
            LocalDateTime.now(),
            correlationId,
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            currency,
            null,
            false
        );
    }

    public static PaymentDomainEvent paymentDebited(UUID paymentId, UUID fromAccountId, UUID toAccountId, 
                                                   long amountMinor, String currency, String requestId, String description) {
        return new PaymentDomainEvent(
            UUID.randomUUID().toString(),
            paymentId,
            PaymentEventType.PAYMENT_DEBITED,
            LocalDateTime.now(),
            requestId,
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            Currency.valueOf(currency),
            null,
            false
        );
    }

    public static PaymentDomainEvent paymentDebited(UUID paymentId, String correlationId, String requestId,
                                                   UUID fromAccountId, UUID toAccountId, long amountMinor, Currency currency) {
        return new PaymentDomainEvent(
            UUID.randomUUID().toString(),
            paymentId,
            PaymentEventType.PAYMENT_DEBITED,
            LocalDateTime.now(),
            correlationId,
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            currency,
            null,
            false
        );
    }

    public static PaymentDomainEvent paymentCredited(UUID paymentId, UUID fromAccountId, UUID toAccountId, 
                                                    long amountMinor, String currency, String requestId, String description) {
        return new PaymentDomainEvent(
            UUID.randomUUID().toString(),
            paymentId,
            PaymentEventType.PAYMENT_CREDITED,
            LocalDateTime.now(),
            requestId,
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            Currency.valueOf(currency),
            null,
            false
        );
    }

    public static PaymentDomainEvent paymentCredited(UUID paymentId, String correlationId, String requestId,
                                                    UUID fromAccountId, UUID toAccountId, long amountMinor, Currency currency) {
        return new PaymentDomainEvent(
            UUID.randomUUID().toString(),
            paymentId,
            PaymentEventType.PAYMENT_CREDITED,
            LocalDateTime.now(),
            correlationId,
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            currency,
            null,
            false
        );
    }

    public static PaymentDomainEvent paymentCompleted(UUID paymentId, UUID fromAccountId, UUID toAccountId, 
                                                     long amountMinor, String currency, String requestId, String description) {
        return new PaymentDomainEvent(
            UUID.randomUUID().toString(),
            paymentId,
            PaymentEventType.PAYMENT_COMPLETED,
            LocalDateTime.now(),
            requestId,
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            Currency.valueOf(currency),
            null,
            false
        );
    }

    public static PaymentDomainEvent paymentCompleted(UUID paymentId, String correlationId, String requestId,
                                                     UUID fromAccountId, UUID toAccountId, long amountMinor, Currency currency) {
        return new PaymentDomainEvent(
            UUID.randomUUID().toString(),
            paymentId,
            PaymentEventType.PAYMENT_COMPLETED,
            LocalDateTime.now(),
            correlationId,
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            currency,
            null,
            false
        );
    }

    public static PaymentDomainEvent paymentFailed(UUID paymentId, UUID fromAccountId, UUID toAccountId, 
                                                  long amountMinor, String currency, String requestId, String failureStatus, String errorMessage) {
        return new PaymentDomainEvent(
            UUID.randomUUID().toString(),
            paymentId,
            PaymentEventType.PAYMENT_FAILED,
            LocalDateTime.now(),
            requestId,
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            Currency.valueOf(currency),
            errorMessage,
            false
        );
    }

    public static PaymentDomainEvent paymentFailed(UUID paymentId, String correlationId, String requestId,
                                                  UUID fromAccountId, UUID toAccountId, long amountMinor, Currency currency,
                                                  String failureReason, boolean compensationRequired) {
        return new PaymentDomainEvent(
            UUID.randomUUID().toString(),
            paymentId,
            PaymentEventType.PAYMENT_FAILED,
            LocalDateTime.now(),
            correlationId,
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            currency,
            failureReason,
            compensationRequired
        );
    }

    public static PaymentDomainEvent paymentCompensated(UUID paymentId, UUID fromAccountId, UUID toAccountId, 
                                                       long amountMinor, String currency, String requestId, String description) {
        return new PaymentDomainEvent(
            UUID.randomUUID().toString(),
            paymentId,
            PaymentEventType.PAYMENT_COMPENSATED,
            LocalDateTime.now(),
            requestId,
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            Currency.valueOf(currency),
            null,
            false
        );
    }

    public static PaymentDomainEvent paymentCompensated(UUID paymentId, String correlationId, String requestId,
                                                       UUID fromAccountId, UUID toAccountId, long amountMinor, Currency currency) {
        return new PaymentDomainEvent(
            UUID.randomUUID().toString(),
            paymentId,
            PaymentEventType.PAYMENT_COMPENSATED,
            LocalDateTime.now(),
            correlationId,
            requestId,
            fromAccountId,
            toAccountId,
            amountMinor,
            currency,
            null,
            false
        );
    }

    // Getters
    public String getEventId() { return eventId; }
    public UUID getPaymentId() { return paymentId; }
    public PaymentEventType getEventType() { return eventType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public LocalDateTime getOccurredAt() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
    public String getRequestId() { return requestId; }
    public UUID getFromAccountId() { return fromAccountId; }
    public UUID getToAccountId() { return toAccountId; }
    public long getAmountMinor() { return amountMinor; }
    public long getAmount() { return amountMinor; }
    public Currency getCurrency() { return currency; }
    public String getFailureReason() { return failureReason; }
    public String getErrorMessage() { return failureReason; }
    public String getDescription() { return requestId; } // Temporary mapping
    public String getPaymentStatus() { return eventType != null ? eventType.name() : null; }
    public boolean isCompensationRequired() { return compensationRequired; }
}