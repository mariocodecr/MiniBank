package com.minibank.accounts.infrastructure.events;

import com.minibank.events.payment.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventProcessor.class);

    public void handlePaymentDebited(PaymentEvent event) {
        logger.info("Processing payment debited event for payment {} from account {}",
            event.getPaymentId(), event.getFromAccountId());
        
        // Here you could:
        // - Update account audit logs
        // - Trigger account balance reconciliation
        // - Update account transaction history
        // - Send account notifications
        
        // For now, this is mainly for audit and monitoring purposes
        // since the actual debit was already performed by the payment service
    }

    public void handlePaymentCredited(PaymentEvent event) {
        logger.info("Processing payment credited event for payment {} to account {}",
            event.getPaymentId(), event.getToAccountId());
        
        // Here you could:
        // - Update account audit logs
        // - Trigger account balance reconciliation
        // - Update account transaction history
        // - Send account notifications
        
        // For now, this is mainly for audit and monitoring purposes
        // since the actual credit was already performed by the payment service
    }

    public void handlePaymentCompleted(PaymentEvent event) {
        logger.info("Processing payment completed event for payment {}", event.getPaymentId());
        
        // Here you could:
        // - Mark transaction as finalized in account records
        // - Trigger final account notifications
        // - Update account statistics
        // - Archive transaction data
    }

    public void handlePaymentFailed(PaymentEvent event) {
        logger.warn("Processing payment failed event for payment {}: {}", 
            event.getPaymentId(), event.getFailureReason());
        
        // Here you could:
        // - Update account failure metrics
        // - Log failed transaction attempts
        // - Trigger account alerts for suspicious activity
        // - Update account risk scoring
    }

    public void handlePaymentCompensated(PaymentEvent event) {
        logger.info("Processing payment compensated event for payment {}", event.getPaymentId());
        
        // Here you could:
        // - Verify compensation was applied correctly
        // - Update account reconciliation records
        // - Log compensation transaction
        // - Trigger account notifications about reversal
    }
}