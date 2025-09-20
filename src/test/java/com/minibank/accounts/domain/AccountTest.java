package com.minibank.accounts.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.minibank.accounts.domain.AccountTestDataFactory.*;
import static com.minibank.accounts.domain.MoneyTestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void shouldCreateNewAccount() {
        UUID userId = UUID.randomUUID();
        Account account = Account.create(userId, Currency.USD);
        
        assertNotNull(account.getId());
        assertEquals(userId, account.getUserId());
        assertEquals(Currency.USD, account.getCurrency());
        assertEquals(0L, account.getBalanceMinor());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertEquals(0L, account.getVersion());
        assertTrue(account.getBalance().isZero());
    }

    @Test
    void shouldCreditAccount() {
        Account account = createAccount();
        Money creditAmount = dollars(5000); // $50.00
        
        account.credit(creditAmount);
        
        assertEquals(5000L, account.getBalanceMinor());
        assertEquals(creditAmount, account.getBalance());
    }

    @Test
    void shouldDebitAccount() {
        Account account = createAccountWithBalance(UUID.randomUUID(), Currency.USD, 10000L);
        Money debitAmount = dollars(3000); // $30.00
        
        account.debit(debitAmount);
        
        assertEquals(7000L, account.getBalanceMinor());
        assertEquals(dollars(7000), account.getBalance());
    }

    @Test
    void shouldThrowExceptionWhenDebitingInsufficientFunds() {
        Account account = createAccountWithBalance(UUID.randomUUID(), Currency.USD, 2000L);
        Money debitAmount = dollars(5000);
        
        assertThrows(IllegalStateException.class, 
            () -> account.debit(debitAmount));
    }

    @Test
    void shouldThrowExceptionWhenOperatingOnSuspendedAccount() {
        Account account = createSuspendedAccount(UUID.randomUUID(), Currency.USD);
        Money amount = dollars(1000);
        
        assertThrows(IllegalStateException.class, 
            () -> account.credit(amount));
        
        assertThrows(IllegalStateException.class, 
            () -> account.debit(amount));
    }

    @Test
    void shouldThrowExceptionWhenOperatingOnClosedAccount() {
        Account account = createClosedAccount(UUID.randomUUID(), Currency.USD);
        Money amount = dollars(1000);
        
        assertThrows(IllegalStateException.class, 
            () -> account.credit(amount));
        
        assertThrows(IllegalStateException.class, 
            () -> account.debit(amount));
    }

    @Test
    void shouldThrowExceptionForCurrencyMismatch() {
        Account account = createAccount(UUID.randomUUID(), Currency.USD);
        Money colonesAmount = colones(1000);
        
        assertThrows(IllegalArgumentException.class, 
            () -> account.credit(colonesAmount));
        
        assertThrows(IllegalArgumentException.class, 
            () -> account.debit(colonesAmount));
    }

    @Test
    void shouldCheckIfCanDebit() {
        Account account = createAccountWithBalance(UUID.randomUUID(), Currency.USD, 5000L);
        
        assertTrue(account.canDebit(dollars(3000)));
        assertTrue(account.canDebit(dollars(5000)));
        assertFalse(account.canDebit(dollars(6000)));
    }

    @Test
    void shouldNotAllowDebitOnSuspendedAccount() {
        Account account = createSuspendedAccount(UUID.randomUUID(), Currency.USD);
        
        assertFalse(account.canDebit(dollars(1000)));
    }

    @Test
    void shouldSuspendAccount() {
        Account account = createAccount();
        
        account.suspend();
        
        assertEquals(AccountStatus.SUSPENDED, account.getStatus());
    }

    @Test
    void shouldActivateAccount() {
        Account account = createSuspendedAccount(UUID.randomUUID(), Currency.USD);
        
        account.activate();
        
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }

    @Test
    void shouldCloseAccountWithZeroBalance() {
        Account account = createAccount();
        
        account.close();
        
        assertEquals(AccountStatus.CLOSED, account.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenClosingAccountWithBalance() {
        Account account = createAccountWithBalance(UUID.randomUUID(), Currency.USD, 1000L);
        
        assertThrows(IllegalStateException.class, account::close);
    }

    @Test
    void shouldThrowExceptionWhenActivatingClosedAccount() {
        Account account = createClosedAccount(UUID.randomUUID(), Currency.USD);
        
        assertThrows(IllegalStateException.class, account::activate);
    }

    @Test
    void shouldThrowExceptionWhenSuspendingClosedAccount() {
        Account account = createClosedAccount(UUID.randomUUID(), Currency.USD);
        
        assertThrows(IllegalStateException.class, account::suspend);
    }
}