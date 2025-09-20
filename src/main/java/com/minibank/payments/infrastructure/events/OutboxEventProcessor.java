package com.minibank.payments.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.events.payment.PaymentEvent;
import com.minibank.payments.domain.events.OutboxEvent;
import com.minibank.payments.domain.events.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class OutboxEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventProcessor.class);
    
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    // Configuration
    @Value("${minibank.events.outbox.batch-size:100}")
    private int batchSize;
    
    @Value("${minibank.events.outbox.retry-delay-ms:5000}")
    private long retryDelayMs;
    
    @Value("${minibank.events.outbox.max-retries:5}")
    private int maxRetries;
    
    // Metrics
    private final Counter eventsPublished;
    private final Counter eventsFailed;
    private final Counter eventsRetried;
    private final Timer publishLatency;
    private final MeterRegistry meterRegistry;

    public OutboxEventProcessor(OutboxEventRepository outboxEventRepository,
                               KafkaTemplate<String, PaymentEvent> kafkaTemplate,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.eventsPublished = Counter.builder("outbox.events.published.total")
            .description("Total number of outbox events published")
            .register(meterRegistry);
        this.eventsFailed = Counter.builder("outbox.events.failed.total")
            .description("Total number of outbox events failed to publish")
            .register(meterRegistry);
        this.eventsRetried = Counter.builder("outbox.events.retried.total")
            .description("Total number of outbox events retried")
            .register(meterRegistry);
        this.publishLatency = Timer.builder("outbox.events.publish.duration.seconds")
            .description("Time taken to publish outbox events")
            .register(meterRegistry);
            
        // Gauge for pending events
        Gauge.builder("outbox.events.pending.count", this, OutboxEventProcessor::getPendingEventsCount)
            .description("Number of pending outbox events")
            .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${minibank.events.outbox.poll-interval-ms:1000}")
    public void processOutboxEvents() {
        Timer.Sample sample = Timer.start();
        
        try {
            List<OutboxEvent> unpublishedEvents = outboxEventRepository.findUnpublishedEvents(batchSize);
            
            if (unpublishedEvents.isEmpty()) {
                return;
            }
            
            logger.debug("Processing {} unpublished outbox events", unpublishedEvents.size());
            
            for (OutboxEvent event : unpublishedEvents) {
                processEvent(event);
            }
            
        } catch (Exception e) {
            logger.error("Error processing outbox events: {}", e.getMessage(), e);
        } finally {
            sample.stop(publishLatency);
        }
    }

    @Transactional
    private void processEvent(OutboxEvent event) {
        try {
            PaymentEvent avroEvent = objectMapper.readValue(event.getPayload(), PaymentEvent.class);
            
            // Publish to Kafka
            CompletableFuture<SendResult<String, PaymentEvent>> future = 
                kafkaTemplate.send("payment-events", event.getPaymentId().toString(), avroEvent);
            
            future.thenAccept(result -> {
                // Mark as published
                event.markAsPublished();
                outboxEventRepository.save(event);
                eventsPublished.increment();
                
                logger.debug("Successfully published outbox event {} for payment {}", 
                    event.getEventId(), event.getPaymentId());
                
            }).exceptionally(throwable -> {
                handlePublishFailure(event, throwable);
                return null;
            });
            
        } catch (Exception e) {
            logger.error("Error processing outbox event {}: {}", event.getEventId(), e.getMessage(), e);
            handlePublishFailure(event, e);
        }
    }

    @Transactional
    private void handlePublishFailure(OutboxEvent event, Throwable error) {
        logger.warn("Failed to publish outbox event {} for payment {}: {}", 
            event.getEventId(), event.getPaymentId(), error.getMessage());
        
        if (event.getRetryCount() < maxRetries) {
            event.incrementRetryCount(error.getMessage());
            outboxEventRepository.save(event);
            eventsRetried.increment();
            
            logger.info("Scheduled retry {} for outbox event {}", 
                event.getRetryCount(), event.getEventId());
        } else {
            logger.error("Max retries exceeded for outbox event {}. Moving to DLQ.", 
                event.getEventId());
            eventsFailed.increment();
            
            // In a real implementation, you might move to a DLQ or mark for manual intervention
            // For now, we'll leave it as failed in the outbox table
        }
    }

    private double getPendingEventsCount() {
        try {
            return outboxEventRepository.findUnpublishedEvents(Integer.MAX_VALUE).size();
        } catch (Exception e) {
            logger.warn("Failed to get pending events count: {}", e.getMessage());
            return -1;
        }
    }

    @Scheduled(cron = "${minibank.events.outbox.cleanup-cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupPublishedEvents() {
        try {
            logger.info("Starting cleanup of published outbox events");
            outboxEventRepository.deletePublishedEventsOlderThan(24); // 24 hours
            logger.info("Completed cleanup of published outbox events");
        } catch (Exception e) {
            logger.error("Error during outbox cleanup: {}", e.getMessage(), e);
        }
    }
}