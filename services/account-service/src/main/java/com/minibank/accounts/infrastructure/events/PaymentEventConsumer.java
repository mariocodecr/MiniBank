package com.minibank.accounts.infrastructure.events;

import java.util.Optional;

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
import com.minibank.accounts.domain.events.InboxEvent;
import com.minibank.accounts.domain.events.InboxEventRepository;
import com.minibank.accounts.infrastructure.events.PaymentEventProcessor.PaymentEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class PaymentEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);
    
    private final InboxEventRepository inboxEventRepository;
    private final PaymentEventProcessor eventProcessor;
    private final ObjectMapper objectMapper;
    
    // Metrics
    private final Counter eventsReceived;
    private final Counter eventsProcessed;
    private final Counter eventsFailed;
    private final Counter eventsDuplicated;
    private final Timer processingLatency;

    public PaymentEventConsumer(InboxEventRepository inboxEventRepository,
                               PaymentEventProcessor eventProcessor,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.inboxEventRepository = inboxEventRepository;
        this.eventProcessor = eventProcessor;
        this.objectMapper = objectMapper;
        
        // Initialize metrics
        this.eventsReceived = Counter.builder("accounts.events.received.total")
            .description("Total number of payment events received by accounts service")
            .register(meterRegistry);
        this.eventsProcessed = Counter.builder("accounts.events.processed.total")
            .description("Total number of payment events processed successfully")
            .register(meterRegistry);
        this.eventsFailed = Counter.builder("accounts.events.failed.total")
            .description("Total number of payment events that failed processing")
            .register(meterRegistry);
        this.eventsDuplicated = Counter.builder("accounts.events.duplicated.total")
            .description("Total number of duplicate payment events ignored")
            .register(meterRegistry);
        this.processingLatency = Timer.builder("accounts.events.processing.duration.seconds")
            .description("Time taken to process payment events")
            .register(meterRegistry);
    }

    @KafkaListener(topics = "payment-events", groupId = "accounts-service")
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
                event.getEventType(),
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
            String eventType = event.getEventType();
            
            switch (eventType) {
                case "PAYMENT_REQUESTED" -> {
                    // No action needed for accounts service on payment request
                    logger.debug("Payment {} requested - no account action required", event.getPaymentId());
                }
                    
                case "PAYMENT_DEBITED" -> {
                    // Account has already been debited by the payment service
                    // This is a confirmation event - could update audit logs
                    logger.info("Payment {} debited from account {} confirmed", 
                        event.getPaymentId(), event.getFromAccountId());
                    eventProcessor.handlePaymentDebited(event);
                }
                    
                case "PAYMENT_CREDITED" -> {
                    // Account has already been credited by the payment service
                    // This is a confirmation event - could update audit logs
                    logger.info("Payment {} credited to account {} confirmed", 
                        event.getPaymentId(), event.getToAccountId());
                    eventProcessor.handlePaymentCredited(event);
                }
                    
                case "PAYMENT_COMPLETED" -> {
                    // Payment saga completed successfully
                    logger.info("Payment {} completed successfully", event.getPaymentId());
                    eventProcessor.handlePaymentCompleted(event);
                }
                    
                case "PAYMENT_FAILED" -> {
                    // Payment failed - might need to handle account state changes
                    logger.warn("Payment {} failed: {}", event.getPaymentId(), event.getFailureReason());
                    eventProcessor.handlePaymentFailed(event);
                }
                    
                case "PAYMENT_COMPENSATED" -> {
                    // Payment was compensated - account balances were reversed
                    logger.info("Payment {} compensated", event.getPaymentId());
                    eventProcessor.handlePaymentCompensated(event);
                }
                    
                default -> {
                    logger.warn("Unknown payment event type: {} for event {}", eventType, event.getEventId());
                    return true; // Consider unknown events as successfully processed
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error processing payment event {}: {}", event.getEventId(), e.getMessage(), e);
            inboxEvent.incrementRetryCount(e.getMessage());
            inboxEventRepository.save(inboxEvent);
            return false;
        }
    }

}