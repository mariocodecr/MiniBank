package com.minibank.payments.application;

import com.minibank.accounts.domain.Currency;
import com.minibank.accounts.domain.Money;
import com.minibank.payments.adapter.client.AccountServiceClient;
import com.minibank.payments.adapter.client.LedgerServiceClient;
import com.minibank.payments.domain.*;
import com.minibank.payments.domain.events.PaymentDomainEvent;
import com.minibank.payments.infrastructure.IdempotencyService;
import com.minibank.payments.infrastructure.events.OutboxEventPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentOrchestrationService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentOrchestrationService.class);

    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;
    private final AccountServiceClient accountServiceClient;
    private final LedgerServiceClient ledgerServiceClient;
    private final OutboxEventPublisher eventPublisher;
    
    // Metrics
    private final Counter paymentsInitiated;
    private final Counter paymentsCompleted;
    private final MeterRegistry meterRegistry;
    private final Counter paymentsCompensated;
    private final Timer paymentLatency;

    public PaymentOrchestrationService(PaymentRepository paymentRepository,
                                     IdempotencyService idempotencyService,
                                     AccountServiceClient accountServiceClient,
                                     LedgerServiceClient ledgerServiceClient,
                                     OutboxEventPublisher eventPublisher,
                                     MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.idempotencyService = idempotencyService;
        this.accountServiceClient = accountServiceClient;
        this.ledgerServiceClient = ledgerServiceClient;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.paymentsInitiated = Counter.builder("payments.initiated.total")
            .description("Total number of payments initiated")
            .register(meterRegistry);
        this.paymentsCompleted = Counter.builder("payments.completed.total")
            .description("Total number of payments completed")
            .register(meterRegistry);
        this.paymentsCompensated = Counter.builder("payments.compensated.total")
            .description("Total number of payments compensated")
            .register(meterRegistry);
        this.paymentLatency = Timer.builder("payments.duration.seconds")
            .description("Payment processing duration")
            .register(meterRegistry);
    }

    public Payment initiatePayment(String requestId, UUID fromAccountId, UUID toAccountId,
                                  long amountMinor, Currency currency) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.info("Initiating payment - requestId: {}, from: {}, to: {}, amount: {}", 
                       requestId, fromAccountId, toAccountId, amountMinor);

            // Check idempotency
            Optional<UUID> existingPaymentId = idempotencyService.getExistingPaymentId(requestId);
            if (existingPaymentId.isPresent()) {
                logger.info("Payment already exists for requestId: {} - paymentId: {}", 
                           requestId, existingPaymentId.get());
                return paymentRepository.findById(existingPaymentId.get())
                    .orElseThrow(() -> new IllegalStateException("Payment not found: " + existingPaymentId.get()));
            }

            // Create payment
            Payment payment = Payment.create(requestId, fromAccountId, toAccountId, amountMinor, currency);
            Payment savedPayment = paymentRepository.save(payment);
            
            // Register idempotency
            idempotencyService.registerPayment(requestId, savedPayment.getId());
            
            // Publish payment initiated event
            PaymentDomainEvent initiatedEvent = PaymentDomainEvent.paymentInitiated(
                savedPayment.getId(), 
                savedPayment.getFromAccountId(),
                savedPayment.getToAccountId(),
                savedPayment.getAmountMinor(),
                savedPayment.getCurrency().name(),
                savedPayment.getRequestId(),
                null
            );
            eventPublisher.publishEvent(initiatedEvent);
            
            paymentsInitiated.increment();
            
            // Start saga orchestration asynchronously (for now, synchronously)
            executePaymentSaga(savedPayment);
            
            return savedPayment;
            
        } finally {
            sample.stop(paymentLatency);
        }
    }

    private void executePaymentSaga(Payment payment) {
        try {
            logger.info("Executing payment saga for payment: {}", payment.getId());
            
            // Step 1: Reserve funds (debit)
            if (payment.canBeDebited()) {
                Money amount = Money.of(payment.getAmountMinor(), payment.getCurrency());
                AccountServiceClient.ReserveResult reserveResult = 
                    accountServiceClient.reserveFunds(payment.getFromAccountId(), amount);
                
                if (reserveResult.isSuccess()) {
                    payment.markAsDebited();
                    paymentRepository.save(payment);
                    
                    // Publish payment debited event
                    PaymentDomainEvent debitedEvent = PaymentDomainEvent.paymentDebited(
                        payment.getId(),
                        payment.getFromAccountId(),
                        payment.getToAccountId(),
                        payment.getAmountMinor(),
                        payment.getCurrency().name(),
                        payment.getRequestId(),
                        null
                    );
                    eventPublisher.publishEvent(debitedEvent);
                    
                    logger.info("Payment {} debited successfully", payment.getId());
                } else {
                    handleDebitFailure(payment, reserveResult.getErrorMessage());
                    return;
                }
            }
            
            // Step 2: Credit destination account
            if (payment.canBeCredited()) {
                Money amount = Money.of(payment.getAmountMinor(), payment.getCurrency());
                AccountServiceClient.PostResult creditResult = 
                    accountServiceClient.postCredit(payment.getToAccountId(), amount);
                
                if (creditResult.isSuccess()) {
                    payment.markAsCredited();
                    paymentRepository.save(payment);
                    
                    // Publish payment credited event
                    PaymentDomainEvent creditedEvent = PaymentDomainEvent.paymentCredited(
                        payment.getId(),
                        payment.getFromAccountId(),
                        payment.getToAccountId(),
                        payment.getAmountMinor(),
                        payment.getCurrency().name(),
                        payment.getRequestId(),
                        null
                    );
                    eventPublisher.publishEvent(creditedEvent);
                    
                    logger.info("Payment {} credited successfully", payment.getId());
                } else {
                    handleCreditFailure(payment, creditResult.getErrorMessage());
                    return;
                }
            }
            
            // Step 3: Record ledger entries
            if (payment.canBeCompleted()) {
                LedgerServiceClient.LedgerResult ledgerResult = 
                    ledgerServiceClient.recordPaymentEntries(
                        payment.getId(),
                        payment.getFromAccountId(),
                        payment.getToAccountId(),
                        payment.getAmountMinor(),
                        payment.getCurrency()
                    );
                
                if (ledgerResult.isSuccess()) {
                    payment.markAsCompleted();
                    paymentRepository.save(payment);
                    
                    // Publish payment completed event
                    PaymentDomainEvent completedEvent = PaymentDomainEvent.paymentCompleted(
                        payment.getId(),
                        payment.getFromAccountId(),
                        payment.getToAccountId(),
                        payment.getAmountMinor(),
                        payment.getCurrency().name(),
                        payment.getRequestId(),
                        null
                    );
                    eventPublisher.publishEvent(completedEvent);
                    
                    paymentsCompleted.increment();
                    logger.info("Payment {} completed successfully", payment.getId());
                } else {
                    handleLedgerFailure(payment, ledgerResult.getErrorMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during payment saga execution for payment: {}", 
                        payment.getId(), e);
            handleSystemError(payment, e.getMessage());
        }
    }

    private void handleDebitFailure(Payment payment, String errorMessage) {
        logger.error("Debit failed for payment {}: {}", payment.getId(), errorMessage);
        
        PaymentStatus failureStatus = determineFailureStatus(errorMessage);
        payment.markAsFailed(failureStatus, errorMessage);
        paymentRepository.save(payment);
        
        // Publish payment failed event
        PaymentDomainEvent failedEvent = PaymentDomainEvent.paymentFailed(
            payment.getId(),
            payment.getFromAccountId(),
            payment.getToAccountId(),
            payment.getAmountMinor(),
            payment.getCurrency().name(),
            payment.getRequestId(),
            failureStatus.name(),
            errorMessage
        );
        eventPublisher.publishEvent(failedEvent);
        
        Counter.builder("payments.failed.total")
            .tag("reason", "debit_failed")
            .register(meterRegistry)
            .increment();
    }

    private void handleCreditFailure(Payment payment, String errorMessage) {
        logger.error("Credit failed for payment {}: {}", payment.getId(), errorMessage);
        
        // Compensate: reverse the debit
        compensateDebit(payment);
        
        Counter.builder("payments.failed.total")
            .tag("reason", "credit_failed")
            .register(meterRegistry)
            .increment();
    }

    private void handleLedgerFailure(Payment payment, String errorMessage) {
        logger.error("Ledger recording failed for payment {}: {}", payment.getId(), errorMessage);
        
        // For now, just mark as system error
        // In a full implementation, we might need compensation for both debit and credit
        payment.markAsFailed(PaymentStatus.FAILED_SYSTEM_ERROR, "Ledger recording failed: " + errorMessage);
        paymentRepository.save(payment);
        
        Counter.builder("payments.failed.total")
            .tag("reason", "ledger_failed")
            .register(meterRegistry)
            .increment();
    }

    private void handleSystemError(Payment payment, String errorMessage) {
        logger.error("System error for payment {}: {}", payment.getId(), errorMessage);
        
        payment.markAsFailed(PaymentStatus.FAILED_SYSTEM_ERROR, errorMessage);
        paymentRepository.save(payment);
        
        Counter.builder("payments.failed.total")
            .tag("reason", "system_error")
            .register(meterRegistry)
            .increment();
    }

    private void compensateDebit(Payment payment) {
        try {
            logger.info("Compensating debit for payment: {}", payment.getId());
            
            Money amount = Money.of(payment.getAmountMinor(), payment.getCurrency());
            AccountServiceClient.PostResult result = 
                accountServiceClient.postCredit(payment.getFromAccountId(), amount);
            
            if (result.isSuccess()) {
                payment.markAsCompensated();
                paymentRepository.save(payment);
                
                // Publish payment compensated event
                PaymentDomainEvent compensatedEvent = PaymentDomainEvent.paymentCompensated(
                    payment.getId(),
                    payment.getFromAccountId(),
                    payment.getToAccountId(),
                    payment.getAmountMinor(),
                    payment.getCurrency().name(),
                    payment.getRequestId(),
                    "Credit failure compensation"
                );
                eventPublisher.publishEvent(compensatedEvent);
                
                paymentsCompensated.increment();
                logger.info("Payment {} compensated successfully", payment.getId());
            } else {
                logger.error("Failed to compensate payment {}: {}", 
                           payment.getId(), result.getErrorMessage());
                // This is a critical error - compensation failed
                payment.markAsFailed(PaymentStatus.FAILED_SYSTEM_ERROR, 
                                   "Compensation failed: " + result.getErrorMessage());
                paymentRepository.save(payment);
            }
        } catch (Exception e) {
            logger.error("Exception during compensation for payment {}", payment.getId(), e);
            payment.markAsFailed(PaymentStatus.FAILED_SYSTEM_ERROR, 
                               "Compensation exception: " + e.getMessage());
            paymentRepository.save(payment);
        }
    }

    private PaymentStatus determineFailureStatus(String errorMessage) {
        if (errorMessage != null) {
            if (errorMessage.contains("insufficient") || errorMessage.contains("overdraft")) {
                return PaymentStatus.FAILED_INSUFFICIENT_FUNDS;
            }
            if (errorMessage.contains("inactive") || errorMessage.contains("suspended")) {
                return PaymentStatus.FAILED_ACCOUNT_INACTIVE;
            }
        }
        return PaymentStatus.FAILED_SYSTEM_ERROR;
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId);
    }
}