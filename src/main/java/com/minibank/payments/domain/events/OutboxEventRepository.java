package com.minibank.payments.domain.events;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent outboxEvent);
    List<OutboxEvent> findUnpublishedEvents(int limit);
    List<OutboxEvent> findEventsByPaymentId(UUID paymentId);
    void deletePublishedEventsOlderThan(int hours);
}