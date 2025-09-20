package com.minibank.payments.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {
    
    @Query("SELECT o FROM OutboxEventEntity o WHERE o.published = false ORDER BY o.createdAt ASC")
    List<OutboxEventEntity> findUnpublishedEventsOrderByCreatedAt(@Param("limit") org.springframework.data.domain.Pageable pageable);
    
    List<OutboxEventEntity> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);
    
    @Modifying
    @Query("DELETE FROM OutboxEventEntity o WHERE o.published = true AND o.publishedAt < :cutoffTime")
    void deletePublishedEventsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    long countByPublished(boolean published);
}