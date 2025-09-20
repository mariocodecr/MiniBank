package com.minibank.payments.domain.saga;

public enum PaymentSagaState {
    STARTED,        // Saga initiated, payment created
    DEBITING,       // Waiting for debit confirmation
    DEBITED,        // Debit confirmed, proceed to credit
    CREDITING,      // Waiting for credit confirmation  
    CREDITED,       // Credit confirmed, proceed to ledger
    COMPLETING,     // Waiting for ledger confirmation
    COMPLETED,      // All steps successful
    COMPENSATING,   // Failure occurred, compensating
    COMPENSATED,    // Compensation completed
    FAILED          // Saga failed completely
}