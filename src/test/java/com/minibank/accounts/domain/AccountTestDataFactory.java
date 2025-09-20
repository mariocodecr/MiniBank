package com.minibank.accounts.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class AccountTestDataFactory {
    
    public static Account createAccount() {
        return createAccount(UUID.randomUUID(), Currency.USD);
    }
    
    public static Account createAccount(UUID userId, Currency currency) {
        return new Account(
            UUID.randomUUID(),
            userId,
            currency,
            0L,
            AccountStatus.ACTIVE,
            0L,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
    
    public static Account createAccountWithBalance(UUID userId, Currency currency, long balanceMinor) {
        return new Account(
            UUID.randomUUID(),
            userId,
            currency,
            balanceMinor,
            AccountStatus.ACTIVE,
            0L,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
    
    public static Account createSuspendedAccount(UUID userId, Currency currency) {
        return new Account(
            UUID.randomUUID(),
            userId,
            currency,
            0L,
            AccountStatus.SUSPENDED,
            0L,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
    
    public static Account createClosedAccount(UUID userId, Currency currency) {
        return new Account(
            UUID.randomUUID(),
            userId,
            currency,
            0L,
            AccountStatus.CLOSED,
            0L,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}