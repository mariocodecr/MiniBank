package com.minibank.payments.infrastructure.events;

import com.minibank.payments.domain.events.PaymentDomainEvent;

public interface OutboxEventPublisher {
    void publishEvent(PaymentDomainEvent domainEvent);
}