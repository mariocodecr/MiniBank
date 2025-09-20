package com.minibank.accounts.infrastructure.events;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.accounts.domain.Currency;
import com.minibank.accounts.domain.CurrencyBalance;
import com.minibank.accounts.domain.MultiCurrencyAccount;
import com.minibank.accounts.domain.events.InboxEvent;
import com.minibank.accounts.domain.events.InboxEventRepository;
import com.minibank.events.account.AccountEvent;
import com.minibank.events.account.AccountEventType;

@Component
public class AccountEventPublisherImpl implements AccountEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(AccountEventPublisherImpl.class);
    
    private final InboxEventRepository outboxEventRepository; // Reusing inbox pattern infrastructure
    private final ObjectMapper objectMapper;

    public AccountEventPublisherImpl(InboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishAccountCreated(MultiCurrencyAccount account) {
        AccountEvent event = AccountEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setAccountId(account.getId().toString())
            .setEventType(AccountEventType.ACCOUNT_CREATED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setCurrency("USD") // Default currency
            .setAmountMinor(0L)
            .setBalanceMinor(0L)
            .setCorrelationId(UUID.randomUUID().toString())
            .build();
        
        publishEvent(event, "ACCOUNT_CREATED");
    }

    @Override
    public void publishCurrencyEnabled(MultiCurrencyAccount account, Currency currency) {
        AccountEvent event = AccountEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setAccountId(account.getId().toString())
            .setEventType(AccountEventType.CURRENCY_ENABLED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setCurrency(currency.getCode())
            .setAmountMinor(0L)
            .setBalanceMinor(0L)
            .setCorrelationId(UUID.randomUUID().toString())
            .build();
        
        publishEvent(event, "CURRENCY_ENABLED");
    }

    @Override
    public void publishBalanceCredited(MultiCurrencyAccount account, Currency currency, 
                                      CurrencyBalance oldBalance, CurrencyBalance newBalance, long amount) {
        AccountEvent event = AccountEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setAccountId(account.getId().toString())
            .setEventType(AccountEventType.BALANCE_CREDITED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setCurrency(currency.getCode())
            .setAmountMinor(amount)
            .setBalanceMinor(newBalance.getAvailableAmountMinor())
            .setCorrelationId(UUID.randomUUID().toString())
            .build();
        
        publishEvent(event, "BALANCE_CREDITED");
    }

    @Override
    public void publishBalanceDebited(MultiCurrencyAccount account, Currency currency, 
                                     CurrencyBalance oldBalance, CurrencyBalance newBalance, long amount) {
        AccountEvent event = AccountEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setAccountId(account.getId().toString())
            .setEventType(AccountEventType.BALANCE_DEBITED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setCurrency(currency.getCode())
            .setAmountMinor(amount)
            .setBalanceMinor(newBalance.getAvailableAmountMinor())
            .setCorrelationId(UUID.randomUUID().toString())
            .build();
        
        publishEvent(event, "BALANCE_DEBITED");
    }

    @Override
    public void publishFundsReserved(MultiCurrencyAccount account, Currency currency, 
                                    CurrencyBalance oldBalance, CurrencyBalance newBalance, long amount) {
        AccountEvent event = AccountEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setAccountId(account.getId().toString())
            .setEventType(AccountEventType.BALANCE_RESERVED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setCurrency(currency.getCode())
            .setAmountMinor(amount)
            .setBalanceMinor(newBalance.getAvailableAmountMinor())
            .setCorrelationId(UUID.randomUUID().toString())
            .build();
        
        publishEvent(event, "BALANCE_RESERVED");
    }

    @Override
    public void publishReservationReleased(MultiCurrencyAccount account, Currency currency, 
                                          CurrencyBalance oldBalance, CurrencyBalance newBalance, long amount) {
        AccountEvent event = AccountEvent.newBuilder()
            .setEventId(UUID.randomUUID().toString())
            .setAccountId(account.getId().toString())
            .setEventType(AccountEventType.BALANCE_RELEASED)
            .setTimestamp(Instant.now().toEpochMilli())
            .setCurrency(currency.getCode())
            .setAmountMinor(amount)
            .setBalanceMinor(newBalance.getAvailableAmountMinor())
            .setCorrelationId(UUID.randomUUID().toString())
            .build();
        
        publishEvent(event, "BALANCE_RELEASED");
    }

    private void publishEvent(AccountEvent event, String eventType) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            InboxEvent outboxEvent = InboxEvent.create(event.getEventId(), eventType, payload);
            outboxEventRepository.save(outboxEvent);
            
            logger.debug("Published account event: {} for account: {}", eventType, event.getAccountId());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize account event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish account event", e);
        }
    }
}