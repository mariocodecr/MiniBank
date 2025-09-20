package com.minibank.payments.domain.saga;

public enum PaymentSagaStep {
    INITIATED,      // Payment created and initiated event published
    DEBIT_REQUESTED,    // Debit operation requested
    CREDIT_REQUESTED,   // Credit operation requested  
    LEDGER_REQUESTED,   // Ledger recording requested
    COMPLETED,          // All operations completed
    COMPENSATION_REQUESTED  // Compensation operations requested
}