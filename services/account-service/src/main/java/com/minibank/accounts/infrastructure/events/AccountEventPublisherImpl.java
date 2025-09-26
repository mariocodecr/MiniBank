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
// TODO: Replace with generated Avro classes when available
// import com.minibank.events.account.AccountEvent;
// import com.minibank.events.account.AccountEventType;

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
            .setAggregateVersion(1L)
            .setCurrency("USD") // Default currency
            .setAmountMinor(0L)
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
            .setAggregateVersion(1L)
            .setCurrency(currency.getCode())
            .setAmountMinor(0L)
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
            .setAggregateVersion(1L)
            .setCurrency(currency.getCode())
            .setAmountMinor(amount)
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
            .setAggregateVersion(1L)
            .setCurrency(currency.getCode())
            .setAmountMinor(amount)
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
            .setAggregateVersion(1L)
            .setCurrency(currency.getCode())
            .setAmountMinor(amount)
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
            .setAggregateVersion(1L)
            .setCurrency(currency.getCode())
            .setAmountMinor(amount)
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

    // Temporary POJO classes - replace with generated Avro classes
    public static class AccountEvent {
        private String eventId;
        private String accountId;
        private AccountEventType eventType;
        private long timestamp;
        private long aggregateVersion;
        private String currency;
        private Long amountMinor;
        private String eventData;

        public static Builder newBuilder() {
            return new Builder();
        }

        public static class Builder {
            private AccountEvent event = new AccountEvent();

            public Builder setEventId(String eventId) {
                event.eventId = eventId;
                return this;
            }

            public Builder setAccountId(String accountId) {
                event.accountId = accountId;
                return this;
            }

            public Builder setEventType(AccountEventType eventType) {
                event.eventType = eventType;
                return this;
            }

            public Builder setTimestamp(long timestamp) {
                event.timestamp = timestamp;
                return this;
            }

            public Builder setAggregateVersion(long aggregateVersion) {
                event.aggregateVersion = aggregateVersion;
                return this;
            }

            public Builder setCurrency(String currency) {
                event.currency = currency;
                return this;
            }

            public Builder setAmountMinor(Long amountMinor) {
                event.amountMinor = amountMinor;
                return this;
            }

            public Builder setEventData(String eventData) {
                event.eventData = eventData;
                return this;
            }

            public AccountEvent build() {
                return event;
            }
        }

        // Getters
        public String getEventId() { return eventId; }
        public String getAccountId() { return accountId; }
        public AccountEventType getEventType() { return eventType; }
        public long getTimestamp() { return timestamp; }
        public long getAggregateVersion() { return aggregateVersion; }
        public String getCurrency() { return currency; }
        public Long getAmountMinor() { return amountMinor; }
        public String getEventData() { return eventData; }
    }

    public enum AccountEventType {
        ACCOUNT_CREATED,
        ACCOUNT_UPDATED,
        ACCOUNT_CLOSED,
        BALANCE_UPDATED,
        CURRENCY_ADDED,
        CURRENCY_ENABLED,
        BALANCE_CREDITED,
        BALANCE_DEBITED,
        BALANCE_RESERVED,
        BALANCE_RELEASED
    }
}