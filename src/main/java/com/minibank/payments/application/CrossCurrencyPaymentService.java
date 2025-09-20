package com.minibank.payments.application;

import com.minibank.accounts.domain.Currency;
import com.minibank.fx.application.FXConversionService;
import com.minibank.fx.application.FXRateLockService;
import com.minibank.fx.domain.FXConversion;
import com.minibank.fx.domain.FXRateLock;
import com.minibank.payments.domain.saga.CrossCurrencyPaymentSaga;
import com.minibank.payments.domain.saga.CrossCurrencyPaymentSagaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CrossCurrencyPaymentService {
    private static final Logger logger = LoggerFactory.getLogger(CrossCurrencyPaymentService.class);

    private final FXRateLockService rateLockService;
    private final FXConversionService conversionService;
    private final CrossCurrencyPaymentSagaRepository sagaRepository;
    
    // Metrics
    private final Counter crossCurrencyPayments;
    private final Counter sameCurrencyPayments;
    private final Counter paymentFailures;
    private final Timer paymentProcessingLatency;

    public CrossCurrencyPaymentService(FXRateLockService rateLockService,
                                     FXConversionService conversionService,
                                     CrossCurrencyPaymentSagaRepository sagaRepository,
                                     MeterRegistry meterRegistry) {
        this.rateLockService = rateLockService;
        this.conversionService = conversionService;
        this.sagaRepository = sagaRepository;
        
        // Initialize metrics
        this.crossCurrencyPayments = Counter.builder("payments.cross.currency.total")
            .description("Total number of cross-currency payments processed")
            .register(meterRegistry);
        this.sameCurrencyPayments = Counter.builder("payments.same.currency.total")
            .description("Total number of same-currency payments processed")
            .register(meterRegistry);
        this.paymentFailures = Counter.builder("payments.failures.total")
            .description("Total number of payment failures")
            .register(meterRegistry);
        this.paymentProcessingLatency = Timer.builder("payments.processing.duration")
            .description("Payment processing latency")
            .register(meterRegistry);
    }

    public CrossCurrencyPaymentSaga initiatePayment(UUID paymentId, String requestId, UUID fromAccountId,
                                                   UUID toAccountId, long fromAmountMinor, Currency fromCurrency,
                                                   Currency toCurrency) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.info("Initiating payment: {} from account {} to {} ({} {} -> {})",
                       paymentId, fromAccountId, toAccountId, fromAmountMinor, fromCurrency.getCode(), toCurrency.getCode());

            // Create saga
            CrossCurrencyPaymentSaga saga = CrossCurrencyPaymentSaga.create(paymentId, requestId, fromAccountId,
                                                                           toAccountId, fromAmountMinor, fromCurrency, toCurrency);
            
            CrossCurrencyPaymentSaga savedSaga = sagaRepository.save(saga);

            if (saga.isCrossCurrency()) {
                crossCurrencyPayments.increment();
                // Start with FX rate locking for cross-currency payments
                processRateLockStep(savedSaga);
            } else {
                sameCurrencyPayments.increment();
                // For same currency, skip FX steps and go directly to debit
                processDebitStep(savedSaga);
            }

            return savedSaga;
            
        } catch (Exception e) {
            logger.error("Failed to initiate payment {}: {}", paymentId, e.getMessage(), e);
            paymentFailures.increment();
            throw new RuntimeException("Payment initiation failed", e);
        } finally {
            sample.stop(paymentProcessingLatency);
        }
    }

    public void processRateLockStep(CrossCurrencyPaymentSaga saga) {
        try {
            logger.info("Processing FX rate lock step for saga {}", saga.getId());
            
            saga.startRateLockStep();
            sagaRepository.save(saga);

            Optional<FXRateLock> rateLock = rateLockService.lockExchangeRate(
                saga.getFromCurrency().getCode(),
                saga.getToCurrency().getCode(),
                saga.getFromAccountId(),
                saga.getRequestId()
            );

            if (rateLock.isPresent()) {
                FXRateLock lock = rateLock.get();
                long convertedAmount = lock.calculateConvertedAmount(saga.getFromAmountMinor());
                
                saga.confirmRateLock(lock.getId(), lock.getLockedRate(), lock.getSpread(),
                                   lock.getProvider(), lock.getExpiresAt().atZone(java.time.ZoneOffset.UTC).toLocalDateTime(),
                                   convertedAmount);
                sagaRepository.save(saga);
                
                logger.info("FX rate locked for saga {}: {} at rate {} expires {}",
                           saga.getId(), lock.getCurrencyPair(), lock.getLockedRate(), lock.getExpiresAt());
                
                // Proceed to debit step
                processDebitStep(saga);
            } else {
                String error = "Failed to lock FX rate for " + saga.getFromCurrency().getCode() + "/" + saga.getToCurrency().getCode();
                handleSagaFailure(saga, error);
            }
            
        } catch (Exception e) {
            logger.error("Error processing FX rate lock step for saga {}: {}", saga.getId(), e.getMessage(), e);
            handleSagaFailure(saga, "FX rate lock step failed: " + e.getMessage());
        }
    }

    public void processDebitStep(CrossCurrencyPaymentSaga saga) {
        try {
            logger.info("Processing debit step for saga {}", saga.getId());
            
            saga.startDebitStep();
            sagaRepository.save(saga);

            // In a real implementation, this would integrate with the account service
            // For now, we'll simulate the debit operation
            simulateAccountDebit(saga);
            
            saga.confirmDebit();
            sagaRepository.save(saga);
            
            logger.info("Debit confirmed for saga {}: {} {} from account {}",
                       saga.getId(), saga.getFromAmountMinor(), saga.getFromCurrency().getCode(), saga.getFromAccountId());

            // Proceed to next step based on whether it's cross-currency
            if (saga.isCrossCurrency()) {
                processFxConversionStep(saga);
            } else {
                processCreditStep(saga);
            }
            
        } catch (Exception e) {
            logger.error("Error processing debit step for saga {}: {}", saga.getId(), e.getMessage(), e);
            handleSagaFailure(saga, "Debit step failed: " + e.getMessage());
        }
    }

    public void processFxConversionStep(CrossCurrencyPaymentSaga saga) {
        try {
            logger.info("Processing FX conversion step for saga {}", saga.getId());
            
            saga.startFxConversionStep();
            sagaRepository.save(saga);

            Optional<FXConversion> conversion = conversionService.convertAmount(
                saga.getFromAccountId(),
                saga.getFromCurrency().getCode(),
                saga.getToCurrency().getCode(),
                saga.getFromAmountMinor(),
                saga.getRequestId()
            );

            if (conversion.isPresent()) {
                saga.confirmFxConversion(conversion.get().getId());
                sagaRepository.save(saga);
                
                logger.info("FX conversion completed for saga {}: {} {} -> {} {}",
                           saga.getId(), saga.getFromAmountMinor(), saga.getFromCurrency().getCode(),
                           conversion.get().getToAmountMinor(), saga.getToCurrency().getCode());
                
                // Proceed to credit step
                processCreditStep(saga);
            } else {
                String error = "FX conversion failed for " + saga.getFromCurrency().getCode() + " to " + saga.getToCurrency().getCode();
                handleSagaFailure(saga, error);
            }
            
        } catch (Exception e) {
            logger.error("Error processing FX conversion step for saga {}: {}", saga.getId(), e.getMessage(), e);
            handleSagaFailure(saga, "FX conversion step failed: " + e.getMessage());
        }
    }

    public void processCreditStep(CrossCurrencyPaymentSaga saga) {
        try {
            logger.info("Processing credit step for saga {}", saga.getId());
            
            saga.startCreditStep();
            sagaRepository.save(saga);

            // In a real implementation, this would integrate with the account service
            // For now, we'll simulate the credit operation
            simulateAccountCredit(saga);
            
            saga.confirmCredit();
            sagaRepository.save(saga);
            
            logger.info("Credit confirmed for saga {}: {} {} to account {}",
                       saga.getId(), saga.getToAmountMinor(), saga.getToCurrency().getCode(), saga.getToAccountId());

            // Proceed to ledger step
            processLedgerStep(saga);
            
        } catch (Exception e) {
            logger.error("Error processing credit step for saga {}: {}", saga.getId(), e.getMessage(), e);
            handleSagaFailure(saga, "Credit step failed: " + e.getMessage());
        }
    }

    public void processLedgerStep(CrossCurrencyPaymentSaga saga) {
        try {
            logger.info("Processing ledger step for saga {}", saga.getId());
            
            saga.startLedgerStep();
            sagaRepository.save(saga);

            // In a real implementation, this would integrate with the ledger service
            // For now, we'll simulate the ledger operation
            simulateLedgerEntry(saga);
            
            saga.complete("SUCCESS");
            sagaRepository.save(saga);
            
            logger.info("Payment completed successfully for saga {}: {} from {} to {}",
                       saga.getId(), saga.getPaymentId(), saga.getFromAccountId(), saga.getToAccountId());
            
        } catch (Exception e) {
            logger.error("Error processing ledger step for saga {}: {}", saga.getId(), e.getMessage(), e);
            handleSagaFailure(saga, "Ledger step failed: " + e.getMessage());
        }
    }

    private void handleSagaFailure(CrossCurrencyPaymentSaga saga, String errorMessage) {
        try {
            logger.warn("Handling saga failure for {}: {}", saga.getId(), errorMessage);
            
            saga.fail(errorMessage);
            sagaRepository.save(saga);
            
            paymentFailures.increment();
            
            // In a real implementation, this would trigger compensation logic
            // For now, we just log the failure
            logger.error("Saga {} failed: {}", saga.getId(), errorMessage);
            
        } catch (Exception e) {
            logger.error("Error handling saga failure for {}: {}", saga.getId(), e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<CrossCurrencyPaymentSaga> getSaga(UUID sagaId) {
        return sagaRepository.findById(sagaId);
    }

    @Transactional(readOnly = true)
    public Optional<CrossCurrencyPaymentSaga> getSagaByPaymentId(UUID paymentId) {
        return sagaRepository.findByPaymentId(paymentId);
    }

    @Transactional(readOnly = true)
    public List<CrossCurrencyPaymentSaga> getActiveSagas() {
        return sagaRepository.findActiveSagas();
    }

    @Transactional(readOnly = true)
    public List<CrossCurrencyPaymentSaga> getSagasForAccount(UUID accountId) {
        return sagaRepository.findByAccountId(accountId);
    }

    // Simulation methods - in real implementation these would call actual services
    private void simulateAccountDebit(CrossCurrencyPaymentSaga saga) {
        // Simulate account debit operation
        logger.debug("Simulating debit of {} {} from account {}",
                    saga.getFromAmountMinor(), saga.getFromCurrency().getCode(), saga.getFromAccountId());
    }

    private void simulateAccountCredit(CrossCurrencyPaymentSaga saga) {
        // Simulate account credit operation
        logger.debug("Simulating credit of {} {} to account {}",
                    saga.getToAmountMinor(), saga.getToCurrency().getCode(), saga.getToAccountId());
    }

    private void simulateLedgerEntry(CrossCurrencyPaymentSaga saga) {
        // Simulate ledger entry creation
        logger.debug("Simulating ledger entry for payment {}", saga.getPaymentId());
    }
}