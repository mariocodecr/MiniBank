package com.minibank.payments.domain;

public enum PaymentStatus {
    REQUESTED,
    DEBITED,
    CREDITED,
    COMPLETED,
    FAILED_INSUFFICIENT_FUNDS,
    FAILED_ACCOUNT_INACTIVE,
    FAILED_SYSTEM_ERROR,
    COMPENSATED
}