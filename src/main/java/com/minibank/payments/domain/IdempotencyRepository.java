package com.minibank.payments.domain;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository {
    void save(IdempotencyKey idempotencyKey);
    Optional<UUID> findPaymentIdByRequestId(String requestId);
    boolean exists(String requestId);
}