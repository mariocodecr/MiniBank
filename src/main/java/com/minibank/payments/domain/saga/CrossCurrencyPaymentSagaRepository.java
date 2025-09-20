package com.minibank.payments.domain.saga;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrossCurrencyPaymentSagaRepository {
    CrossCurrencyPaymentSaga save(CrossCurrencyPaymentSaga saga);
    
    Optional<CrossCurrencyPaymentSaga> findById(UUID id);
    
    Optional<CrossCurrencyPaymentSaga> findByPaymentId(UUID paymentId);
    
    List<CrossCurrencyPaymentSaga> findByAccountId(UUID accountId);
    
    List<CrossCurrencyPaymentSaga> findByRequestId(String requestId);
    
    List<CrossCurrencyPaymentSaga> findActiveSagas();
    
    List<CrossCurrencyPaymentSaga> findSagasInState(CrossCurrencyPaymentSagaState state);
    
    List<CrossCurrencyPaymentSaga> findExpiredRateLockSagas();
}