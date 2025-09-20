package com.minibank.payments.domain.saga;

import com.minibank.accounts.domain.Currency;

import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentSaga {
    private UUID id;
    private UUID paymentId;
    private String requestId;
    private PaymentSagaState sagaState;
    private UUID fromAccountId;
    private UUID toAccountId;
    private long amountMinor;
    private Currency currency;
    private PaymentSagaStep currentStep;
    private String completionStatus;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime lastRetryAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected PaymentSaga() {}

    public PaymentSaga(UUID id, UUID paymentId, String requestId, PaymentSagaState sagaState,
                      UUID fromAccountId, UUID toAccountId, long amountMinor, Currency currency,
                      PaymentSagaStep currentStep, String completionStatus, String errorMessage,
                      int retryCount, LocalDateTime lastRetryAt, LocalDateTime startedAt,
                      LocalDateTime completedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.requestId = requestId;
        this.sagaState = sagaState;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.currentStep = currentStep;
        this.completionStatus = completionStatus;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.lastRetryAt = lastRetryAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentSaga create(UUID paymentId, String requestId, UUID fromAccountId,
                                   UUID toAccountId, long amountMinor, Currency currency) {
        LocalDateTime now = LocalDateTime.now();
        return new PaymentSaga(
            UUID.randomUUID(),
            paymentId,
            requestId,
            PaymentSagaState.STARTED,
            fromAccountId,
            toAccountId,
            amountMinor,
            currency,
            PaymentSagaStep.INITIATED,
            null,
            null,
            0,
            null,
            now,
            null,
            now,
            now
        );
    }

    // State transition methods
    public void startDebitStep() {
        this.sagaState = PaymentSagaState.DEBITING;
        this.currentStep = PaymentSagaStep.DEBIT_REQUESTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void confirmDebit() {
        if (sagaState != PaymentSagaState.DEBITING) {
            throw new IllegalStateException("Cannot confirm debit from state: " + sagaState);
        }
        this.sagaState = PaymentSagaState.DEBITED;
        this.updatedAt = LocalDateTime.now();
    }

    public void startCreditStep() {
        if (sagaState != PaymentSagaState.DEBITED) {
            throw new IllegalStateException("Cannot start credit from state: " + sagaState);
        }
        this.sagaState = PaymentSagaState.CREDITING;
        this.currentStep = PaymentSagaStep.CREDIT_REQUESTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void confirmCredit() {
        if (sagaState != PaymentSagaState.CREDITING) {
            throw new IllegalStateException("Cannot confirm credit from state: " + sagaState);
        }
        this.sagaState = PaymentSagaState.CREDITED;
        this.updatedAt = LocalDateTime.now();
    }

    public void startLedgerStep() {
        if (sagaState != PaymentSagaState.CREDITED) {
            throw new IllegalStateException("Cannot start ledger from state: " + sagaState);
        }
        this.sagaState = PaymentSagaState.COMPLETING;
        this.currentStep = PaymentSagaStep.LEDGER_REQUESTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete(String completionStatus) {
        if (sagaState != PaymentSagaState.COMPLETING) {
            throw new IllegalStateException("Cannot complete from state: " + sagaState);
        }
        this.sagaState = PaymentSagaState.COMPLETED;
        this.currentStep = PaymentSagaStep.COMPLETED;
        this.completionStatus = completionStatus;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.sagaState = PaymentSagaState.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void startCompensation(String errorMessage) {
        this.sagaState = PaymentSagaState.COMPENSATING;
        this.currentStep = PaymentSagaStep.COMPENSATION_REQUESTED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public void completeCompensation() {
        if (sagaState != PaymentSagaState.COMPENSATING) {
            throw new IllegalStateException("Cannot complete compensation from state: " + sagaState);
        }
        this.sagaState = PaymentSagaState.COMPENSATED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementRetryCount(String errorMessage) {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canRetry(int maxRetries) {
        return retryCount < maxRetries;
    }

    public boolean isActive() {
        return sagaState == PaymentSagaState.STARTED || 
               sagaState == PaymentSagaState.DEBITING || 
               sagaState == PaymentSagaState.DEBITED ||
               sagaState == PaymentSagaState.CREDITING || 
               sagaState == PaymentSagaState.CREDITED ||
               sagaState == PaymentSagaState.COMPLETING ||
               sagaState == PaymentSagaState.COMPENSATING;
    }

    public boolean isCompleted() {
        return sagaState == PaymentSagaState.COMPLETED || 
               sagaState == PaymentSagaState.COMPENSATED || 
               sagaState == PaymentSagaState.FAILED;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public String getRequestId() { return requestId; }
    public PaymentSagaState getSagaState() { return sagaState; }
    public UUID getFromAccountId() { return fromAccountId; }
    public UUID getToAccountId() { return toAccountId; }
    public long getAmountMinor() { return amountMinor; }
    public Currency getCurrency() { return currency; }
    public PaymentSagaStep getCurrentStep() { return currentStep; }
    public String getCompletionStatus() { return completionStatus; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }
    public LocalDateTime getLastRetryAt() { return lastRetryAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}