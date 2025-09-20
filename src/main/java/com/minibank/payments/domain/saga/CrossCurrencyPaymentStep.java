package com.minibank.payments.domain.saga;

public enum CrossCurrencyPaymentStep {
    INITIATED,                  // Payment created and initiated event published
    FX_RATE_LOCK_REQUESTED,    // FX rate lock requested
    DEBIT_REQUESTED,           // Debit operation requested (from account in original currency)
    FX_CONVERSION_REQUESTED,   // FX conversion operation requested
    CREDIT_REQUESTED,          // Credit operation requested (to account in target currency)
    LEDGER_REQUESTED,          // Ledger recording requested
    COMPLETED,                 // All operations completed
    COMPENSATION_REQUESTED     // Compensation operations requested
}