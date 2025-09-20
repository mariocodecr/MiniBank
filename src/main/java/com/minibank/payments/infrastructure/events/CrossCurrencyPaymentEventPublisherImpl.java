package com.minibank.payments.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.accounts.domain.events.InboxEvent;
import com.minibank.accounts.domain.events.InboxEventRepository;
import com.minibank.events.payment.CrossCurrencyPaymentEvent;
import com.minibank.events.payment.CrossCurrencyPaymentEventType;
import com.minibank.events.payment.FXConversionDetails;
import com.minibank.events.payment.FXConversionStatus;
import com.minibank.events.payment.PaymentRoute;
import com.minibank.fx.domain.FXConversion;
import com.minibank.fx.domain.FXRateLock;
import com.minibank.payments.domain.saga.CrossCurrencyPaymentSaga;

@Component
public class CrossCurrencyPaymentEventPublisherImpl implements CrossCurrencyPaymentEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(CrossCurrencyPaymentEventPublisherImpl.class);

    private final InboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public CrossCurrencyPaymentEventPublisherImpl(InboxEventRepository outboxEventRepository, 
                                                 ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishFxQuoteRequested(CrossCurrencyPaymentSaga saga) {
        CrossCurrencyPaymentEvent event = CrossCurrencyPaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setPaymentId(saga.getPaymentId().toString())
            .setCorrelationId(saga.getRequestId())
            .setRequestId(saga.getRequestId())
            .setEventType(CrossCurrencyPaymentEventType.FX_QUOTE_REQUESTED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setFromAccountId(saga.getFromAccountId().toString())
            .setToAccountId(saga.getToAccountId().toString())
            .setSourceCurrency(saga.getFromCurrency().getCode())
            .setDestinationCurrency(saga.getToCurrency().getCode())
            .setSourceAmount(saga.getFromAmountMinor())
            .setDestinationAmount(0L)
            .setFxConversionDetails(null)
            .setPaymentRoute(PaymentRoute.CROSS_BORDER)
            .setFailureReason(null)
            .setCompensationRequired(false)
            .build();

        publishEvent(event, "FX_QUOTE_REQUESTED");
    }

    @Override
    public void publishFxQuoteProvided(CrossCurrencyPaymentSaga saga, FXRateLock rateLock) {
        FXConversionDetails conversionDetails = FXConversionDetails.newBuilder()
            .setConversionId(rateLock.getId().toString())
            .setRateQuoteId(rateLock.getId().toString())
            .setRate(rateLock.getLockedRate().toString())
            .setSpread(rateLock.getSpread().toString())
            .setProviderId("internal")
            .setFxRateId(rateLock.getId().toString())
            .setConversionStatus(FXConversionStatus.LOCKED)
            .build();

        CrossCurrencyPaymentEvent event = CrossCurrencyPaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setPaymentId(saga.getPaymentId().toString())
            .setCorrelationId(saga.getRequestId())
            .setRequestId(saga.getRequestId())
            .setEventType(CrossCurrencyPaymentEventType.FX_QUOTE_PROVIDED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setFromAccountId(saga.getFromAccountId().toString())
            .setToAccountId(saga.getToAccountId().toString())
            .setSourceCurrency(saga.getFromCurrency().getCode())
            .setDestinationCurrency(saga.getToCurrency().getCode())
            .setSourceAmount(saga.getFromAmountMinor())
            .setDestinationAmount(saga.getToAmountMinor())
            .setFxConversionDetails(conversionDetails)
            .setPaymentRoute(PaymentRoute.CROSS_BORDER)
            .setFailureReason(null)
            .setCompensationRequired(false)
            .build();

        publishEvent(event, "FX_QUOTE_PROVIDED");
    }

    @Override
    public void publishFxRateLocked(CrossCurrencyPaymentSaga saga) {
        FXConversionDetails conversionDetails = createFxConversionDetails(saga, FXConversionStatus.LOCKED);

        CrossCurrencyPaymentEvent event = CrossCurrencyPaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setPaymentId(saga.getPaymentId().toString())
            .setCorrelationId(saga.getRequestId())
            .setRequestId(saga.getRequestId())
            .setEventType(CrossCurrencyPaymentEventType.FX_RATE_LOCKED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setFromAccountId(saga.getFromAccountId().toString())
            .setToAccountId(saga.getToAccountId().toString())
            .setSourceCurrency(saga.getFromCurrency().getCode())
            .setDestinationCurrency(saga.getToCurrency().getCode())
            .setSourceAmount(saga.getFromAmountMinor())
            .setDestinationAmount(saga.getToAmountMinor())
            .setFxConversionDetails(conversionDetails)
            .setPaymentRoute(PaymentRoute.CROSS_BORDER)
            .setFailureReason(null)
            .setCompensationRequired(false)
            .build();

        publishEvent(event, "FX_RATE_LOCKED");
    }

    @Override
    public void publishFxRateExpired(CrossCurrencyPaymentSaga saga) {
        FXConversionDetails conversionDetails = createFxConversionDetails(saga, FXConversionStatus.EXPIRED);

        CrossCurrencyPaymentEvent event = CrossCurrencyPaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setPaymentId(saga.getPaymentId().toString())
            .setCorrelationId(saga.getRequestId())
            .setRequestId(saga.getRequestId())
            .setEventType(CrossCurrencyPaymentEventType.FX_RATE_EXPIRED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setFromAccountId(saga.getFromAccountId().toString())
            .setToAccountId(saga.getToAccountId().toString())
            .setSourceCurrency(saga.getFromCurrency().getCode())
            .setDestinationCurrency(saga.getToCurrency().getCode())
            .setSourceAmount(saga.getFromAmountMinor())
            .setDestinationAmount(saga.getToAmountMinor())
            .setFxConversionDetails(conversionDetails)
            .setPaymentRoute(PaymentRoute.CROSS_BORDER)
            .setFailureReason("Rate lock expired")
            .setCompensationRequired(true)
            .build();

        publishEvent(event, "FX_RATE_EXPIRED");
    }

    @Override
    public void publishFxConversionStarted(CrossCurrencyPaymentSaga saga) {
        FXConversionDetails conversionDetails = createFxConversionDetails(saga, FXConversionStatus.PENDING);

        CrossCurrencyPaymentEvent event = CrossCurrencyPaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setPaymentId(saga.getPaymentId().toString())
            .setCorrelationId(saga.getRequestId())
            .setRequestId(saga.getRequestId())
            .setEventType(CrossCurrencyPaymentEventType.FX_CONVERSION_STARTED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setFromAccountId(saga.getFromAccountId().toString())
            .setToAccountId(saga.getToAccountId().toString())
            .setSourceCurrency(saga.getFromCurrency().getCode())
            .setDestinationCurrency(saga.getToCurrency().getCode())
            .setSourceAmount(saga.getFromAmountMinor())
            .setDestinationAmount(saga.getToAmountMinor())
            .setFxConversionDetails(conversionDetails)
            .setPaymentRoute(PaymentRoute.CROSS_BORDER)
            .setFailureReason(null)
            .setCompensationRequired(false)
            .build();

        publishEvent(event, "FX_CONVERSION_STARTED");
    }

    @Override
    public void publishFxConversionCompleted(CrossCurrencyPaymentSaga saga, FXConversion conversion) {
        FXConversionDetails conversionDetails = FXConversionDetails.newBuilder()
            .setConversionId(conversion.getId().toString())
            .setRateQuoteId(saga.getFxRateLockId() != null ? saga.getFxRateLockId().toString() : null)
            .setRate(conversion.getExchangeRate().toString())
            .setSpread(conversion.getSpread().toString())
            .setProviderId("internal")
            .setFxRateId(saga.getFxRateLockId() != null ? saga.getFxRateLockId().toString() : "none")
            .setConversionStatus(FXConversionStatus.CONVERTED)
            .build();

        CrossCurrencyPaymentEvent event = CrossCurrencyPaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setPaymentId(saga.getPaymentId().toString())
            .setCorrelationId(saga.getRequestId())
            .setRequestId(saga.getRequestId())
            .setEventType(CrossCurrencyPaymentEventType.FX_CONVERSION_COMPLETED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setFromAccountId(saga.getFromAccountId().toString())
            .setToAccountId(saga.getToAccountId().toString())
            .setSourceCurrency(saga.getFromCurrency().getCode())
            .setDestinationCurrency(saga.getToCurrency().getCode())
            .setSourceAmount(saga.getFromAmountMinor())
            .setDestinationAmount(saga.getToAmountMinor())
            .setFxConversionDetails(conversionDetails)
            .setPaymentRoute(PaymentRoute.CROSS_BORDER)
            .setFailureReason(null)
            .setCompensationRequired(false)
            .build();

        publishEvent(event, "FX_CONVERSION_COMPLETED");
    }

    @Override
    public void publishFxConversionFailed(CrossCurrencyPaymentSaga saga, String failureReason) {
        FXConversionDetails conversionDetails = createFxConversionDetails(saga, FXConversionStatus.FAILED);

        CrossCurrencyPaymentEvent event = CrossCurrencyPaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setPaymentId(saga.getPaymentId().toString())
            .setCorrelationId(saga.getRequestId())
            .setRequestId(saga.getRequestId())
            .setEventType(CrossCurrencyPaymentEventType.FX_CONVERSION_FAILED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setFromAccountId(saga.getFromAccountId().toString())
            .setToAccountId(saga.getToAccountId().toString())
            .setSourceCurrency(saga.getFromCurrency().getCode())
            .setDestinationCurrency(saga.getToCurrency().getCode())
            .setSourceAmount(saga.getFromAmountMinor())
            .setDestinationAmount(saga.getToAmountMinor())
            .setFxConversionDetails(conversionDetails)
            .setPaymentRoute(PaymentRoute.CROSS_BORDER)
            .setFailureReason(failureReason)
            .setCompensationRequired(true)
            .build();

        publishEvent(event, "FX_CONVERSION_FAILED");
    }

    @Override
    public void publishCrossCurrencyPaymentCompleted(CrossCurrencyPaymentSaga saga) {
        FXConversionDetails conversionDetails = saga.isCrossCurrency() ? 
            createFxConversionDetails(saga, FXConversionStatus.CONVERTED) : null;

        CrossCurrencyPaymentEvent event = CrossCurrencyPaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setPaymentId(saga.getPaymentId().toString())
            .setCorrelationId(saga.getRequestId())
            .setRequestId(saga.getRequestId())
            .setEventType(CrossCurrencyPaymentEventType.CROSS_CURRENCY_PAYMENT_COMPLETED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setFromAccountId(saga.getFromAccountId().toString())
            .setToAccountId(saga.getToAccountId().toString())
            .setSourceCurrency(saga.getFromCurrency().getCode())
            .setDestinationCurrency(saga.getToCurrency().getCode())
            .setSourceAmount(saga.getFromAmountMinor())
            .setDestinationAmount(saga.getToAmountMinor())
            .setFxConversionDetails(conversionDetails)
            .setPaymentRoute(saga.isCrossCurrency() ? PaymentRoute.CROSS_BORDER : PaymentRoute.DOMESTIC)
            .setFailureReason(null)
            .setCompensationRequired(false)
            .build();

        publishEvent(event, "CROSS_CURRENCY_PAYMENT_COMPLETED");
    }

    @Override
    public void publishCrossCurrencyPaymentFailed(CrossCurrencyPaymentSaga saga, String failureReason) {
        FXConversionDetails conversionDetails = saga.isCrossCurrency() ? 
            createFxConversionDetails(saga, FXConversionStatus.FAILED) : null;

        CrossCurrencyPaymentEvent event = CrossCurrencyPaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setPaymentId(saga.getPaymentId().toString())
            .setCorrelationId(saga.getRequestId())
            .setRequestId(saga.getRequestId())
            .setEventType(CrossCurrencyPaymentEventType.CROSS_CURRENCY_PAYMENT_FAILED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setFromAccountId(saga.getFromAccountId().toString())
            .setToAccountId(saga.getToAccountId().toString())
            .setSourceCurrency(saga.getFromCurrency().getCode())
            .setDestinationCurrency(saga.getToCurrency().getCode())
            .setSourceAmount(saga.getFromAmountMinor())
            .setDestinationAmount(saga.getToAmountMinor())
            .setFxConversionDetails(conversionDetails)
            .setPaymentRoute(saga.isCrossCurrency() ? PaymentRoute.CROSS_BORDER : PaymentRoute.DOMESTIC)
            .setFailureReason(failureReason)
            .setCompensationRequired(true)
            .build();

        publishEvent(event, "CROSS_CURRENCY_PAYMENT_FAILED");
    }

    @Override
    public void publishCrossCurrencyPaymentCompensated(CrossCurrencyPaymentSaga saga) {
        FXConversionDetails conversionDetails = saga.isCrossCurrency() ? 
            createFxConversionDetails(saga, FXConversionStatus.FAILED) : null;

        CrossCurrencyPaymentEvent event = CrossCurrencyPaymentEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setPaymentId(saga.getPaymentId().toString())
            .setCorrelationId(saga.getRequestId())
            .setRequestId(saga.getRequestId())
            .setEventType(CrossCurrencyPaymentEventType.CROSS_CURRENCY_PAYMENT_COMPENSATED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setFromAccountId(saga.getFromAccountId().toString())
            .setToAccountId(saga.getToAccountId().toString())
            .setSourceCurrency(saga.getFromCurrency().getCode())
            .setDestinationCurrency(saga.getToCurrency().getCode())
            .setSourceAmount(saga.getFromAmountMinor())
            .setDestinationAmount(saga.getToAmountMinor())
            .setFxConversionDetails(conversionDetails)
            .setPaymentRoute(saga.isCrossCurrency() ? PaymentRoute.CROSS_BORDER : PaymentRoute.DOMESTIC)
            .setFailureReason(saga.getErrorMessage())
            .setCompensationRequired(false)
            .build();

        publishEvent(event, "CROSS_CURRENCY_PAYMENT_COMPENSATED");
    }

    private FXConversionDetails createFxConversionDetails(CrossCurrencyPaymentSaga saga, FXConversionStatus status) {
        if (saga.getFxRateLockId() == null) {
            return null;
        }

        return FXConversionDetails.newBuilder()
            .setConversionId(saga.getFxConversionId() != null ? saga.getFxConversionId().toString() : UUID.randomUUID().toString())
            .setRateQuoteId(saga.getFxRateLockId().toString())
            .setRate(saga.getLockedExchangeRate() != null ? saga.getLockedExchangeRate().toString() : "1.0")
            .setSpread(saga.getFxSpread() != null ? saga.getFxSpread().toString() : "0.0")
            .setProviderId("internal")
            .setFxRateId(saga.getFxRateLockId() != null ? saga.getFxRateLockId().toString() : "none")
            .setConversionStatus(status)
            .build();
    }

    private void publishEvent(CrossCurrencyPaymentEvent event, String eventType) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            InboxEvent outboxEvent = InboxEvent.create(event.getEventId(), eventType, payload);
            outboxEventRepository.save(outboxEvent);

            logger.debug("Published cross-currency payment event: {} for payment: {}", 
                        eventType, event.getPaymentId());

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize cross-currency payment event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish cross-currency payment event", e);
        }
    }
}