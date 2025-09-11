package com.minibank.payments.domain;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID paymentId);
    Optional<Payment> findByRequestId(String requestId);
    boolean existsByRequestId(String requestId);
}