package com.minibank.accounts.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

public class InboxEvent {
    private UUID id;
    private String eventId;
    private String eventType;
    private String payload;
    private boolean processed;
    private LocalDateTime processedAt;
    private int retryCount;
    private LocalDateTime lastRetryAt;
    private String errorMessage;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected InboxEvent() {}

    public InboxEvent(UUID id, String eventId, String eventType, String payload, boolean processed,
                     LocalDateTime processedAt, int retryCount, LocalDateTime lastRetryAt, 
                     String errorMessage, LocalDateTime receivedAt, LocalDateTime createdAt, 
                     LocalDateTime updatedAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.processed = processed;
        this.processedAt = processedAt;
        this.retryCount = retryCount;
        this.lastRetryAt = lastRetryAt;
        this.errorMessage = errorMessage;
        this.receivedAt = receivedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static InboxEvent create(String eventId, String eventType, String payload) {
        LocalDateTime now = LocalDateTime.now();
        return new InboxEvent(
            UUID.randomUUID(),
            eventId,
            eventType,
            payload,
            false,
            null,
            0,
            null,
            null,
            now,
            now,
            now
        );
    }

    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementRetryCount(String errorMessage) {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canRetry(int maxRetries) {
        return retryCount < maxRetries;
    }

    // Getters
    public UUID getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public boolean isProcessed() { return processed; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public int getRetryCount() { return retryCount; }
    public LocalDateTime getLastRetryAt() { return lastRetryAt; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}