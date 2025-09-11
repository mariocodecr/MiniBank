package com.minibank.payments.infrastructure;

import com.minibank.payments.domain.IdempotencyKey;
import com.minibank.payments.domain.IdempotencyRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {
    private final IdempotencyRepository idempotencyRepository;

    public IdempotencyService(IdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    public Optional<UUID> getExistingPaymentId(String requestId) {
        return idempotencyRepository.findPaymentIdByRequestId(requestId);
    }

    public void registerPayment(String requestId, UUID paymentId) {
        IdempotencyKey idempotencyKey = IdempotencyKey.create(requestId, paymentId);
        idempotencyRepository.save(idempotencyKey);
    }

    public boolean isRequestProcessed(String requestId) {
        return idempotencyRepository.exists(requestId);
    }
}