package com.minibank.payments.domain.saga;

public enum CrossCurrencyPaymentSagaState {
    STARTED,              // Saga initiated, payment created
    LOCKING_FX_RATE,     // Requesting FX rate lock
    FX_RATE_LOCKED,      // FX rate locked, proceed to debit
    DEBITING,            // Waiting for debit confirmation (from currency)
    DEBITED,             // Debit confirmed, proceed to FX conversion
    CONVERTING_CURRENCY, // Performing FX conversion
    CURRENCY_CONVERTED,  // FX conversion completed, proceed to credit
    CREDITING,           // Waiting for credit confirmation (to currency)
    CREDITED,            // Credit confirmed, proceed to ledger
    COMPLETING,          // Waiting for ledger confirmation
    COMPLETED,           // All steps successful
    COMPENSATING,        // Failure occurred, compensating
    COMPENSATED,         // Compensation completed
    FAILED               // Saga failed completely
}