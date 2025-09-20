package com.minibank.payments.adapter.persistence;

import com.minibank.payments.domain.events.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventMapper {

    public OutboxEventEntity toEntity(OutboxEvent domainObject) {
        if (domainObject == null) {
            return null;
        }

        return new OutboxEventEntity(
            domainObject.getId(),
            domainObject.getEventId(),
            domainObject.getPaymentId(),
            domainObject.getEventType(),
            domainObject.getCorrelationId(),
            domainObject.getPayload(),
            domainObject.getCreatedAt(),
            domainObject.getPublishedAt(),
            domainObject.isPublished(),
            domainObject.getRetryCount(),
            domainObject.getLastRetryAt(),
            domainObject.getErrorMessage()
        );
    }

    public OutboxEvent toDomainObject(OutboxEventEntity entity) {
        if (entity == null) {
            return null;
        }

        return new OutboxEvent(
            entity.getId(),
            entity.getEventId(),
            entity.getPaymentId(),
            entity.getEventType(),
            entity.getCorrelationId(),
            entity.getPayload(),
            entity.getCreatedAt(),
            entity.getPublishedAt(),
            entity.getPublished(),
            entity.getRetryCount(),
            entity.getLastRetryAt(),
            entity.getErrorMessage()
        );
    }
}