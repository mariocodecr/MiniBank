package com.minibank.accounts.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InboxEventJpaRepository extends JpaRepository<InboxEventEntity, UUID> {
    
    Optional<InboxEventEntity> findByEventId(String eventId);
    
    @Query("SELECT i FROM InboxEventEntity i WHERE i.processed = false ORDER BY i.receivedAt ASC")
    List<InboxEventEntity> findUnprocessedEventsOrderByReceivedAt(@Param("limit") org.springframework.data.domain.Pageable pageable);
    
    @Modifying
    @Query("DELETE FROM InboxEventEntity i WHERE i.processed = true AND i.processedAt < :cutoffTime")
    void deleteProcessedEventsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    long countByProcessed(boolean processed);
}