package com.minibank.accounts.adapter.persistence;

import com.minibank.accounts.domain.events.InboxEvent;
import org.springframework.stereotype.Component;

@Component
public class InboxEventMapper {

    public InboxEventEntity toEntity(InboxEvent domainObject) {
        if (domainObject == null) {
            return null;
        }

        return new InboxEventEntity(
            domainObject.getId(),
            domainObject.getEventId(),
            domainObject.getEventType(),
            domainObject.getPayload(),
            domainObject.isProcessed(),
            domainObject.getProcessedAt(),
            domainObject.getRetryCount(),
            domainObject.getLastRetryAt(),
            domainObject.getErrorMessage(),
            domainObject.getReceivedAt(),
            domainObject.getCreatedAt(),
            domainObject.getUpdatedAt()
        );
    }

    public InboxEvent toDomainObject(InboxEventEntity entity) {
        if (entity == null) {
            return null;
        }

        return new InboxEvent(
            entity.getId(),
            entity.getEventId(),
            entity.getEventType(),
            entity.getPayload(),
            entity.getProcessed(),
            entity.getProcessedAt(),
            entity.getRetryCount(),
            entity.getLastRetryAt(),
            entity.getErrorMessage(),
            entity.getReceivedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}