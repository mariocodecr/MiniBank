package com.minibank.payments.infrastructure.events;

import com.minibank.fx.domain.FXConversion;
import com.minibank.fx.domain.FXRateLock;
import com.minibank.payments.domain.saga.CrossCurrencyPaymentSaga;

public interface CrossCurrencyPaymentEventPublisher {
    void publishFxQuoteRequested(CrossCurrencyPaymentSaga saga);
    
    void publishFxQuoteProvided(CrossCurrencyPaymentSaga saga, FXRateLock rateLock);
    
    void publishFxRateLocked(CrossCurrencyPaymentSaga saga);
    
    void publishFxRateExpired(CrossCurrencyPaymentSaga saga);
    
    void publishFxConversionStarted(CrossCurrencyPaymentSaga saga);
    
    void publishFxConversionCompleted(CrossCurrencyPaymentSaga saga, FXConversion conversion);
    
    void publishFxConversionFailed(CrossCurrencyPaymentSaga saga, String failureReason);
    
    void publishCrossCurrencyPaymentCompleted(CrossCurrencyPaymentSaga saga);
    
    void publishCrossCurrencyPaymentFailed(CrossCurrencyPaymentSaga saga, String failureReason);
    
    void publishCrossCurrencyPaymentCompensated(CrossCurrencyPaymentSaga saga);
}