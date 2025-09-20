package com.minibank.payments.domain.saga;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentSagaRepository {
    PaymentSaga save(PaymentSaga saga);
    Optional<PaymentSaga> findById(UUID id);
    Optional<PaymentSaga> findByPaymentId(UUID paymentId);
    Optional<PaymentSaga> findByRequestId(String requestId);
    List<PaymentSaga> findActiveSagas();
    List<PaymentSaga> findSagasByState(PaymentSagaState state);
    void deleteCompletedSagasOlderThan(int hours);
}