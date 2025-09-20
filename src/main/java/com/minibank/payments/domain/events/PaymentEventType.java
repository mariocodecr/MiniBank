package com.minibank.payments.domain.events;

public enum PaymentEventType {
    PAYMENT_REQUESTED,
    PAYMENT_DEBITED, 
    PAYMENT_CREDITED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    PAYMENT_COMPENSATED
}