package com.minibank.accounts.adapter.persistence;

import com.minibank.accounts.domain.events.InboxEvent;
import com.minibank.accounts.domain.events.InboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class InboxEventRepositoryImpl implements InboxEventRepository {

    private final InboxEventJpaRepository jpaRepository;
    private final InboxEventMapper mapper;

    public InboxEventRepositoryImpl(InboxEventJpaRepository jpaRepository, InboxEventMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public InboxEvent save(InboxEvent inboxEvent) {
        InboxEventEntity entity = mapper.toEntity(inboxEvent);
        InboxEventEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomainObject(savedEntity);
    }

    @Override
    public Optional<InboxEvent> findByEventId(String eventId) {
        return jpaRepository.findByEventId(eventId)
                .map(mapper::toDomainObject);
    }

    @Override
    public List<InboxEvent> findUnprocessedEvents(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<InboxEventEntity> entities = jpaRepository.findUnprocessedEventsOrderByReceivedAt(pageable);
        return entities.stream()
                .map(mapper::toDomainObject)
                .toList();
    }

    @Override
    public void deleteProcessedEventsOlderThan(int hours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        jpaRepository.deleteProcessedEventsOlderThan(cutoffTime);
    }

    @Override
    public long countByProcessed(boolean processed) {
        return jpaRepository.countByProcessed(processed);
    }
}