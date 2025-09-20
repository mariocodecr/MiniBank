package com.minibank.payments.domain.saga;

import com.minibank.accounts.domain.Currency;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CrossCurrencyPaymentSaga {
    private UUID id;
    private UUID paymentId;
    private String requestId;
    private CrossCurrencyPaymentSagaState sagaState;
    private UUID fromAccountId;
    private UUID toAccountId;
    private long fromAmountMinor;
    private long toAmountMinor;
    private Currency fromCurrency;
    private Currency toCurrency;
    private CrossCurrencyPaymentStep currentStep;
    
    // FX-specific fields
    private UUID fxRateLockId;
    private BigDecimal lockedExchangeRate;
    private BigDecimal fxSpread;
    private String fxProvider;
    private LocalDateTime rateLockExpiresAt;
    private UUID fxConversionId;
    
    private String completionStatus;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime lastRetryAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected CrossCurrencyPaymentSaga() {}

    public CrossCurrencyPaymentSaga(UUID id, UUID paymentId, String requestId, CrossCurrencyPaymentSagaState sagaState,
                                   UUID fromAccountId, UUID toAccountId, long fromAmountMinor, long toAmountMinor,
                                   Currency fromCurrency, Currency toCurrency, CrossCurrencyPaymentStep currentStep,
                                   UUID fxRateLockId, BigDecimal lockedExchangeRate, BigDecimal fxSpread,
                                   String fxProvider, LocalDateTime rateLockExpiresAt, UUID fxConversionId,
                                   String completionStatus, String errorMessage, int retryCount, 
                                   LocalDateTime lastRetryAt, LocalDateTime startedAt, LocalDateTime completedAt,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.requestId = requestId;
        this.sagaState = sagaState;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.fromAmountMinor = fromAmountMinor;
        this.toAmountMinor = toAmountMinor;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.currentStep = currentStep;
        this.fxRateLockId = fxRateLockId;
        this.lockedExchangeRate = lockedExchangeRate;
        this.fxSpread = fxSpread;
        this.fxProvider = fxProvider;
        this.rateLockExpiresAt = rateLockExpiresAt;
        this.fxConversionId = fxConversionId;
        this.completionStatus = completionStatus;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.lastRetryAt = lastRetryAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CrossCurrencyPaymentSaga create(UUID paymentId, String requestId, UUID fromAccountId,
                                                 UUID toAccountId, long fromAmountMinor, Currency fromCurrency,
                                                 Currency toCurrency) {
        LocalDateTime now = LocalDateTime.now();
        return new CrossCurrencyPaymentSaga(
            UUID.randomUUID(), paymentId, requestId, CrossCurrencyPaymentSagaState.STARTED,
            fromAccountId, toAccountId, fromAmountMinor, 0L, fromCurrency, toCurrency,
            CrossCurrencyPaymentStep.INITIATED, null, null, null, null, null, null,
            null, null, 0, null, now, null, now, now
        );
    }

    // FX Rate Locking Step
    public void startRateLockStep() {
        if (sagaState != CrossCurrencyPaymentSagaState.STARTED) {
            throw new IllegalStateException("Cannot start rate lock from state: " + sagaState);
        }
        this.sagaState = CrossCurrencyPaymentSagaState.LOCKING_FX_RATE;
        this.currentStep = CrossCurrencyPaymentStep.FX_RATE_LOCK_REQUESTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void confirmRateLock(UUID rateLockId, BigDecimal exchangeRate, BigDecimal spread,
                               String provider, LocalDateTime expiresAt, long convertedAmount) {
        if (sagaState != CrossCurrencyPaymentSagaState.LOCKING_FX_RATE) {
            throw new IllegalStateException("Cannot confirm rate lock from state: " + sagaState);
        }
        this.sagaState = CrossCurrencyPaymentSagaState.FX_RATE_LOCKED;
        this.fxRateLockId = rateLockId;
        this.lockedExchangeRate = exchangeRate;
        this.fxSpread = spread;
        this.fxProvider = provider;
        this.rateLockExpiresAt = expiresAt;
        this.toAmountMinor = convertedAmount;
        this.updatedAt = LocalDateTime.now();
    }

    // Debit Step (from original currency account)
    public void startDebitStep() {
        if (sagaState != CrossCurrencyPaymentSagaState.FX_RATE_LOCKED) {
            throw new IllegalStateException("Cannot start debit from state: " + sagaState);
        }
        this.sagaState = CrossCurrencyPaymentSagaState.DEBITING;
        this.currentStep = CrossCurrencyPaymentStep.DEBIT_REQUESTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void confirmDebit() {
        if (sagaState != CrossCurrencyPaymentSagaState.DEBITING) {
            throw new IllegalStateException("Cannot confirm debit from state: " + sagaState);
        }
        this.sagaState = CrossCurrencyPaymentSagaState.DEBITED;
        this.updatedAt = LocalDateTime.now();
    }

    // FX Conversion Step
    public void startFxConversionStep() {
        if (sagaState != CrossCurrencyPaymentSagaState.DEBITED) {
            throw new IllegalStateException("Cannot start FX conversion from state: " + sagaState);
        }
        this.sagaState = CrossCurrencyPaymentSagaState.CONVERTING_CURRENCY;
        this.currentStep = CrossCurrencyPaymentStep.FX_CONVERSION_REQUESTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void confirmFxConversion(UUID conversionId) {
        if (sagaState != CrossCurrencyPaymentSagaState.CONVERTING_CURRENCY) {
            throw new IllegalStateException("Cannot confirm FX conversion from state: " + sagaState);
        }
        this.sagaState = CrossCurrencyPaymentSagaState.CURRENCY_CONVERTED;
        this.fxConversionId = conversionId;
        this.updatedAt = LocalDateTime.now();
    }

    // Credit Step (to target currency account)
    public void startCreditStep() {
        if (sagaState != CrossCurrencyPaymentSagaState.CURRENCY_CONVERTED) {
            throw new IllegalStateException("Cannot start credit from state: " + sagaState);
        }
        this.sagaState = CrossCurrencyPaymentSagaState.CREDITING;
        this.currentStep = CrossCurrencyPaymentStep.CREDIT_REQUESTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void confirmCredit() {
        if (sagaState != CrossCurrencyPaymentSagaState.CREDITING) {
            throw new IllegalStateException("Cannot confirm credit from state: " + sagaState);
        }
        this.sagaState = CrossCurrencyPaymentSagaState.CREDITED;
        this.updatedAt = LocalDateTime.now();
    }

    // Ledger Step
    public void startLedgerStep() {
        if (sagaState != CrossCurrencyPaymentSagaState.CREDITED) {
            throw new IllegalStateException("Cannot start ledger from state: " + sagaState);
        }
        this.sagaState = CrossCurrencyPaymentSagaState.COMPLETING;
        this.currentStep = CrossCurrencyPaymentStep.LEDGER_REQUESTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete(String completionStatus) {
        if (sagaState != CrossCurrencyPaymentSagaState.COMPLETING) {
            throw new IllegalStateException("Cannot complete from state: " + sagaState);
        }
        this.sagaState = CrossCurrencyPaymentSagaState.COMPLETED;
        this.currentStep = CrossCurrencyPaymentStep.COMPLETED;
        this.completionStatus = completionStatus;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Failure and Compensation
    public void fail(String errorMessage) {
        this.sagaState = CrossCurrencyPaymentSagaState.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void startCompensation(String errorMessage) {
        this.sagaState = CrossCurrencyPaymentSagaState.COMPENSATING;
        this.currentStep = CrossCurrencyPaymentStep.COMPENSATION_REQUESTED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public void completeCompensation() {
        if (sagaState != CrossCurrencyPaymentSagaState.COMPENSATING) {
            throw new IllegalStateException("Cannot complete compensation from state: " + sagaState);
        }
        this.sagaState = CrossCurrencyPaymentSagaState.COMPENSATED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Retry logic
    public void incrementRetryCount(String errorMessage) {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canRetry(int maxRetries) {
        return retryCount < maxRetries;
    }

    // Status checks
    public boolean isActive() {
        return sagaState == CrossCurrencyPaymentSagaState.STARTED ||
               sagaState == CrossCurrencyPaymentSagaState.LOCKING_FX_RATE ||
               sagaState == CrossCurrencyPaymentSagaState.FX_RATE_LOCKED ||
               sagaState == CrossCurrencyPaymentSagaState.DEBITING ||
               sagaState == CrossCurrencyPaymentSagaState.DEBITED ||
               sagaState == CrossCurrencyPaymentSagaState.CONVERTING_CURRENCY ||
               sagaState == CrossCurrencyPaymentSagaState.CURRENCY_CONVERTED ||
               sagaState == CrossCurrencyPaymentSagaState.CREDITING ||
               sagaState == CrossCurrencyPaymentSagaState.CREDITED ||
               sagaState == CrossCurrencyPaymentSagaState.COMPLETING ||
               sagaState == CrossCurrencyPaymentSagaState.COMPENSATING;
    }

    public boolean isCompleted() {
        return sagaState == CrossCurrencyPaymentSagaState.COMPLETED ||
               sagaState == CrossCurrencyPaymentSagaState.COMPENSATED ||
               sagaState == CrossCurrencyPaymentSagaState.FAILED;
    }

    public boolean isRateLockExpired() {
        return rateLockExpiresAt != null && LocalDateTime.now().isAfter(rateLockExpiresAt);
    }

    public boolean isCrossCurrency() {
        return !fromCurrency.equals(toCurrency);
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public String getRequestId() { return requestId; }
    public CrossCurrencyPaymentSagaState getSagaState() { return sagaState; }
    public UUID getFromAccountId() { return fromAccountId; }
    public UUID getToAccountId() { return toAccountId; }
    public long getFromAmountMinor() { return fromAmountMinor; }
    public long getToAmountMinor() { return toAmountMinor; }
    public Currency getFromCurrency() { return fromCurrency; }
    public Currency getToCurrency() { return toCurrency; }
    public CrossCurrencyPaymentStep getCurrentStep() { return currentStep; }
    public UUID getFxRateLockId() { return fxRateLockId; }
    public BigDecimal getLockedExchangeRate() { return lockedExchangeRate; }
    public BigDecimal getFxSpread() { return fxSpread; }
    public String getFxProvider() { return fxProvider; }
    public LocalDateTime getRateLockExpiresAt() { return rateLockExpiresAt; }
    public UUID getFxConversionId() { return fxConversionId; }
    public String getCompletionStatus() { return completionStatus; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }
    public LocalDateTime getLastRetryAt() { return lastRetryAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}