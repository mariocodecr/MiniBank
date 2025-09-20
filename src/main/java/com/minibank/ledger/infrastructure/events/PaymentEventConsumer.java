package com.minibank.ledger.infrastructure.events;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.events.payment.PaymentEvent;
import com.minibank.events.payment.PaymentEventType;
import com.minibank.ledger.application.LedgerService;
import com.minibank.ledger.domain.events.InboxEvent;
import com.minibank.ledger.domain.events.InboxEventRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class PaymentEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);
    
    private final InboxEventRepository inboxEventRepository;
    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;
    
    // Metrics
    private final Counter eventsReceived;
    private final Counter eventsProcessed;
    private final Counter eventsFailed;
    private final Counter eventsDuplicated;
    private final Timer processingLatency;

    public PaymentEventConsumer(InboxEventRepository inboxEventRepository,
                               LedgerService ledgerService,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.inboxEventRepository = inboxEventRepository;
        this.ledgerService = ledgerService;
        this.objectMapper = objectMapper;
        
        // Initialize metrics
        this.eventsReceived = Counter.builder("ledger.events.received.total")
            .description("Total number of payment events received by ledger service")
            .register(meterRegistry);
        this.eventsProcessed = Counter.builder("ledger.events.processed.total")
            .description("Total number of payment events processed successfully")
            .register(meterRegistry);
        this.eventsFailed = Counter.builder("ledger.events.failed.total")
            .description("Total number of payment events that failed processing")
            .register(meterRegistry);
        this.eventsDuplicated = Counter.builder("ledger.events.duplicated.total")
            .description("Total number of duplicate payment events ignored")
            .register(meterRegistry);
        this.processingLatency = Timer.builder("ledger.events.processing.duration.seconds")
            .description("Time taken to process payment events")
            .register(meterRegistry);
    }

    @KafkaListener(topics = "payment-events", groupId = "ledger-service")
    @Transactional
    public void handlePaymentEvent(@Payload PaymentEvent event,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset,
                                  Acknowledgment acknowledgment) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.debug("Received payment event: {} for payment: {} from topic: {} partition: {} offset: {}",
                event.getEventType(), event.getPaymentId(), topic, partition, offset);
            
            eventsReceived.increment();
            
            // Check for duplicates using event ID
            Optional<InboxEvent> existingEvent = inboxEventRepository.findByEventId(event.getEventId());
            if (existingEvent.isPresent()) {
                logger.debug("Duplicate event {} already processed, ignoring", event.getEventId());
                eventsDuplicated.increment();
                acknowledgment.acknowledge();
                return;
            }
            
            // Store in inbox for idempotent processing
            String payload = objectMapper.writeValueAsString(event);
            InboxEvent inboxEvent = InboxEvent.create(
                event.getEventId(),
                event.getEventType().name(),
                payload
            );
            inboxEventRepository.save(inboxEvent);
            
            // Process the event
            boolean processedSuccessfully = processEvent(event, inboxEvent);
            
            if (processedSuccessfully) {
                inboxEvent.markAsProcessed();
                inboxEventRepository.save(inboxEvent);
                eventsProcessed.increment();
                acknowledgment.acknowledge();
                
                logger.info("Successfully processed payment event {} for payment {}",
                    event.getEventType(), event.getPaymentId());
            } else {
                eventsFailed.increment();
                logger.error("Failed to process payment event {} for payment {}",
                    event.getEventType(), event.getPaymentId());
                // Don't acknowledge - let Kafka retry
            }
            
        } catch (JsonProcessingException e) {
            logger.error("JSON processing error for payment event {}: {}", event.getEventId(), e.getMessage(), e);
            eventsFailed.increment();
            // Don't acknowledge - let Kafka retry
        } catch (IllegalArgumentException e) {
            logger.error("Invalid payment event data {}: {}", event.getEventId(), e.getMessage(), e);
            eventsFailed.increment();
            // Don't acknowledge - let Kafka retry
        } catch (RuntimeException e) {
            logger.error("Runtime error handling payment event {}: {}", event.getEventId(), e.getMessage(), e);
            eventsFailed.increment();
            // Don't acknowledge - let Kafka retry
        } catch (Exception e) {
            logger.error("Unexpected error handling payment event {}: {}", event.getEventId(), e.getMessage(), e);
            eventsFailed.increment();
            // Don't acknowledge - let Kafka retry
        } finally {
            sample.stop(processingLatency);
        }
    }

    private boolean processEvent(PaymentEvent event, InboxEvent inboxEvent) {
        try {
            PaymentEventType eventType = event.getEventType();
            
            switch (eventType) {
                case PAYMENT_COMPLETED -> {
                    // Record ledger entries for the completed payment
                    logger.info("Recording ledger entries for completed payment {}", event.getPaymentId());
                    return recordLedgerEntries(event);
                }
                    
                case PAYMENT_COMPENSATED -> {
                    // Record compensating ledger entries
                    logger.info("Recording compensating ledger entries for payment {}", event.getPaymentId());
                    return recordCompensatingEntries(event);
                }
                    
                case PAYMENT_FAILED -> {
                    // Log the failed payment for audit purposes
                    logger.warn("Payment {} failed: {}", event.getPaymentId(), event.getFailureReason());
                    return recordFailedPayment(event);
                }
                    
                default -> {
                    // Other payment events don't require ledger action
                    logger.debug("Payment event {} for payment {} - no ledger action required",
                        eventType, event.getPaymentId());
                    return true;
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing payment event {}: {}", event.getEventId(), e.getMessage(), e);
            inboxEvent.incrementRetryCount(e.getMessage());
            inboxEventRepository.save(inboxEvent);
            return false;
        }
    }

    private boolean recordLedgerEntries(PaymentEvent event) {
        try {
            UUID paymentId = UUID.fromString(event.getPaymentId());
            UUID fromAccountId = UUID.fromString(event.getFromAccountId());
            UUID toAccountId = UUID.fromString(event.getToAccountId());
            
            // Use the existing ledger service to record entries
            var entries = ledgerService.recordPaymentEntries(
                paymentId,
                fromAccountId,
                toAccountId,
                event.getAmountMinor(),
                com.minibank.accounts.domain.Currency.valueOf(event.getCurrency().toString())
            );
            
            return entries != null && !entries.isEmpty();
            
        } catch (Exception e) {
            logger.error("Failed to record ledger entries for payment {}: {}", 
                event.getPaymentId(), e.getMessage(), e);
            return false;
        }
    }

    private boolean recordCompensatingEntries(PaymentEvent event) {
        try {
            // Record compensating entries (reverse the original transaction)
            UUID paymentId = UUID.fromString(event.getPaymentId());
            UUID fromAccountId = UUID.fromString(event.getFromAccountId());
            UUID toAccountId = UUID.fromString(event.getToAccountId());
            
            // Reverse the entries: credit back to from account, debit from to account
            var entries = ledgerService.recordPaymentEntries(
                paymentId,
                toAccountId,  // Reversed
                fromAccountId,  // Reversed
                event.getAmountMinor(),
                com.minibank.accounts.domain.Currency.valueOf(event.getCurrency().toString())
            );
            
            return entries != null && !entries.isEmpty();
            
        } catch (Exception e) {
            logger.error("Failed to record compensating ledger entries for payment {}: {}", 
                event.getPaymentId(), e.getMessage(), e);
            return false;
        }
    }

    private boolean recordFailedPayment(PaymentEvent event) {
        try {
            // Log failed payment for audit trail
            logger.warn("Failed payment audit: paymentId={}, fromAccount={}, toAccount={}, amount={}, currency={}, reason={}",
                event.getPaymentId(), event.getFromAccountId(), event.getToAccountId(),
                event.getAmountMinor(), event.getCurrency(), event.getFailureReason());
            
            // Could also create a specific audit ledger entry here
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to record failed payment audit for payment {}: {}", 
                event.getPaymentId(), e.getMessage(), e);
            return false;
        }
    }
}