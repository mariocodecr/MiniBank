package com.minibank.payments.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

public class OutboxEvent {
    private UUID id;
    private String eventId;
    private UUID paymentId;
    private String eventType;
    private String correlationId;
    private String payload;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private boolean published;
    private int retryCount;
    private LocalDateTime lastRetryAt;
    private String errorMessage;

    protected OutboxEvent() {}

    public OutboxEvent(UUID id, String eventId, UUID paymentId, String eventType, String correlationId,
                      String payload, LocalDateTime createdAt, LocalDateTime publishedAt, boolean published,
                      int retryCount, LocalDateTime lastRetryAt, String errorMessage) {
        this.id = id;
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.payload = payload;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.published = published;
        this.retryCount = retryCount;
        this.lastRetryAt = lastRetryAt;
        this.errorMessage = errorMessage;
    }

    public static OutboxEvent create(PaymentDomainEvent domainEvent, String payload) {
        return new OutboxEvent(
            UUID.randomUUID(),
            domainEvent.getEventId(),
            domainEvent.getPaymentId(),
            domainEvent.getEventType().name(),
            domainEvent.getCorrelationId(),
            payload,
            LocalDateTime.now(),
            null,
            false,
            0,
            null,
            null
        );
    }

    public void markAsPublished() {
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }

    public void incrementRetryCount(String errorMessage) {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    // Getters
    public UUID getId() { return id; }
    public String getEventId() { return eventId; }
    public UUID getPaymentId() { return paymentId; }
    public String getEventType() { return eventType; }
    public String getCorrelationId() { return correlationId; }
    public String getPayload() { return payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public boolean isPublished() { return published; }
    public int getRetryCount() { return retryCount; }
    public LocalDateTime getLastRetryAt() { return lastRetryAt; }
    public String getErrorMessage() { return errorMessage; }
}