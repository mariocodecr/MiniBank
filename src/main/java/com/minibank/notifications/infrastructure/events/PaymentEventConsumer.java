package com.minibank.notifications.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.notifications.domain.events.InboxEvent;
import com.minibank.notifications.domain.events.InboxEventRepository;
import com.minibank.events.payment.PaymentEvent;
import com.minibank.events.payment.PaymentEventType;
import com.minibank.notifications.application.NotificationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class PaymentEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);
    
    private final InboxEventRepository inboxEventRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    
    // Metrics
    private final Counter eventsReceived;
    private final Counter eventsProcessed;
    private final Counter eventsFailed;
    private final Counter eventsDuplicated;
    private final Timer processingLatency;

    public PaymentEventConsumer(InboxEventRepository inboxEventRepository,
                               NotificationService notificationService,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.inboxEventRepository = inboxEventRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        
        // Initialize metrics
        this.eventsReceived = Counter.builder("notifications.events.received.total")
            .description("Total number of payment events received by notifications service")
            .register(meterRegistry);
        this.eventsProcessed = Counter.builder("notifications.events.processed.total")
            .description("Total number of payment events processed successfully")
            .register(meterRegistry);
        this.eventsFailed = Counter.builder("notifications.events.failed.total")
            .description("Total number of payment events that failed processing")
            .register(meterRegistry);
        this.eventsDuplicated = Counter.builder("notifications.events.duplicated.total")
            .description("Total number of duplicate payment events ignored")
            .register(meterRegistry);
        this.processingLatency = Timer.builder("notifications.events.processing.duration.seconds")
            .description("Time taken to process payment events")
            .register(meterRegistry);
    }

    @KafkaListener(topics = "payment-events", groupId = "notifications-service")
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
            
        } catch (Exception e) {
            logger.error("Error handling payment event {}: {}", event.getEventId(), e.getMessage(), e);
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
                case PAYMENT_COMPLETED:
                    logger.info("Sending payment completion notification for payment {}", event.getPaymentId());
                    return notificationService.sendPaymentCompletedNotification(event);
                    
                case PAYMENT_FAILED:
                    logger.info("Sending payment failure notification for payment {}", event.getPaymentId());
                    return notificationService.sendPaymentFailedNotification(event);
                    
                case PAYMENT_DEBITED:
                    logger.info("Sending debit notification for payment {}", event.getPaymentId());
                    return notificationService.sendDebitNotification(event);
                    
                case PAYMENT_CREDITED:
                    logger.info("Sending credit notification for payment {}", event.getPaymentId());
                    return notificationService.sendCreditNotification(event);
                    
                case PAYMENT_COMPENSATED:
                    logger.info("Sending compensation notification for payment {}", event.getPaymentId());
                    return notificationService.sendCompensationNotification(event);
                    
                default:
                    // Some payment events may not require notifications
                    logger.debug("Payment event {} for payment {} - no notification required",
                        eventType, event.getPaymentId());
                    return true;
            }
            
        } catch (Exception e) {
            logger.error("Error processing payment event {}: {}", event.getEventId(), e.getMessage(), e);
            inboxEvent.incrementRetryCount(e.getMessage());
            inboxEventRepository.save(inboxEvent);
            return false;
        }
    }
}