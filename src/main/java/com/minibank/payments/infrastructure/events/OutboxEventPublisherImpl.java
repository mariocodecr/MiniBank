package com.minibank.payments.infrastructure.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.events.payment.PaymentEvent;
import com.minibank.events.payment.PaymentEventType;
import com.minibank.payments.domain.events.OutboxEvent;
import com.minibank.payments.domain.events.OutboxEventRepository;
import com.minibank.payments.domain.events.PaymentDomainEvent;

@Component
public class OutboxEventPublisherImpl implements OutboxEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventPublisherImpl.class);
    
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisherImpl(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishEvent(PaymentDomainEvent domainEvent) {
        try {
            PaymentEvent avroEvent = createAvroEvent(domainEvent);
            String payload = objectMapper.writeValueAsString(avroEvent);
            
            OutboxEvent outboxEvent = OutboxEvent.create(domainEvent, payload);
            outboxEventRepository.save(outboxEvent);
            
            logger.debug("Saved outbox event for payment {} with event type {}", 
                domainEvent.getPaymentId(), domainEvent.getEventType());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize payment event for payment {}: {}", 
                domainEvent.getPaymentId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish payment event", e);
        }
    }

    private PaymentEvent createAvroEvent(PaymentDomainEvent domainEvent) {
        return PaymentEvent.newBuilder()
            .setEventId(domainEvent.getEventId())
            .setPaymentId(domainEvent.getPaymentId().toString())
            .setEventType(PaymentEventType.valueOf(domainEvent.getEventType().name()))
            .setTimestamp(java.time.Instant.from(domainEvent.getOccurredAt().atZone(java.time.ZoneOffset.UTC)).toEpochMilli())
            .setCorrelationId(domainEvent.getCorrelationId())
            .setRequestId(domainEvent.getRequestId())
            .setFromAccountId(domainEvent.getFromAccountId() != null ? domainEvent.getFromAccountId().toString() : "")
            .setToAccountId(domainEvent.getToAccountId() != null ? domainEvent.getToAccountId().toString() : "")
            .setAmountMinor(domainEvent.getAmountMinor())
            .setCurrency(domainEvent.getCurrency().name())
            .setFailureReason(domainEvent.getFailureReason())
            .setCompensationRequired(domainEvent.isCompensationRequired())
            .build();
    }
}