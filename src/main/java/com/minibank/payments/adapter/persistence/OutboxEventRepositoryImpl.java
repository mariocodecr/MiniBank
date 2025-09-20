package com.minibank.payments.adapter.persistence;

import com.minibank.payments.domain.events.OutboxEvent;
import com.minibank.payments.domain.events.OutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository jpaRepository;
    private final OutboxEventMapper mapper;

    public OutboxEventRepositoryImpl(OutboxEventJpaRepository jpaRepository, OutboxEventMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        OutboxEventEntity entity = mapper.toEntity(outboxEvent);
        OutboxEventEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomainObject(savedEntity);
    }

    @Override
    public List<OutboxEvent> findUnpublishedEvents(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<OutboxEventEntity> entities = jpaRepository.findUnpublishedEventsOrderByCreatedAt(pageable);
        return entities.stream()
                .map(mapper::toDomainObject)
                .toList();
    }

    @Override
    public List<OutboxEvent> findEventsByPaymentId(UUID paymentId) {
        List<OutboxEventEntity> entities = jpaRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId);
        return entities.stream()
                .map(mapper::toDomainObject)
                .toList();
    }

    @Override
    public void deletePublishedEventsOlderThan(int hours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        jpaRepository.deletePublishedEventsOlderThan(cutoffTime);
    }
}