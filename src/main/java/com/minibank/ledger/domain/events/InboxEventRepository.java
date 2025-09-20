package com.minibank.ledger.domain.events;

import java.util.List;
import java.util.Optional;

public interface InboxEventRepository {
    InboxEvent save(InboxEvent inboxEvent);
    Optional<InboxEvent> findByEventId(String eventId);
    List<InboxEvent> findUnprocessedEvents(int limit);
    void deleteProcessedEventsOlderThan(int hours);
    long countByProcessed(boolean processed);
}