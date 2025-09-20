package com.minibank.payments.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class IdempotencyKey {
    private String requestId;
    private UUID paymentId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    protected IdempotencyKey() {}

    public IdempotencyKey(String requestId, UUID paymentId, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.requestId = requestId;
        this.paymentId = paymentId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static IdempotencyKey create(String requestId, UUID paymentId) {
        LocalDateTime now = LocalDateTime.now();
        return new IdempotencyKey(
            requestId,
            paymentId,
            now,
            now.plusHours(24) // 24h TTL
        );
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // Getters
    public String getRequestId() { return requestId; }
    public UUID getPaymentId() { return paymentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}