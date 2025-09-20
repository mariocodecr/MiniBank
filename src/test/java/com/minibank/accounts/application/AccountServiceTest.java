package com.minibank.accounts.application;

import com.minibank.accounts.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.minibank.accounts.domain.AccountTestDataFactory.*;
import static com.minibank.accounts.domain.MoneyTestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository);
    }

    @Test
    void shouldCreateAccount() {
        UUID userId = UUID.randomUUID();
        Currency currency = Currency.USD;
        Account expectedAccount = createAccount(userId, currency);
        
        when(accountRepository.existsByUserIdAndCurrency(userId, currency)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(expectedAccount);

        Account result = accountService.createAccount(userId, currency);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(currency, result.getCurrency());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void shouldThrowExceptionWhenUserAlreadyHasAccountInCurrency() {
        UUID userId = UUID.randomUUID();
        Currency currency = Currency.USD;
        
        when(accountRepository.existsByUserIdAndCurrency(userId, currency)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
            () -> accountService.createAccount(userId, currency));
        
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void shouldGetAccountById() {
        UUID accountId = UUID.randomUUID();
        Account expectedAccount = createAccount();
        
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(expectedAccount));

        Optional<Account> result = accountService.getAccount(accountId);

        assertTrue(result.isPresent());
        assertEquals(expectedAccount, result.get());
    }

    @Test
    void shouldReturnEmptyWhenAccountNotFound() {
        UUID accountId = UUID.randomUUID();
        
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        Optional<Account> result = accountService.getAccount(accountId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGetAccountsByUserId() {
        UUID userId = UUID.randomUUID();
        List<Account> expectedAccounts = List.of(
            createAccount(userId, Currency.USD),
            createAccount(userId, Currency.CRC)
        );
        
        when(accountRepository.findByUserId(userId)).thenReturn(expectedAccounts);

        List<Account> result = accountService.getAccountsByUserId(userId);

        assertEquals(2, result.size());
        assertEquals(expectedAccounts, result);
    }

    @Test
    void shouldReserveFunds() {
        UUID accountId = UUID.randomUUID();
        Account account = createAccountWithBalance(UUID.randomUUID(), Currency.USD, 10000L);
        Money reserveAmount = dollars(5000);
        Account updatedAccount = createAccountWithBalance(account.getUserId(), Currency.USD, 5000L);
        
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);

        Account result = accountService.reserveFunds(accountId, reserveAmount);

        assertEquals(5000L, result.getBalanceMinor());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void shouldThrowExceptionWhenReservingFromNonExistentAccount() {
        UUID accountId = UUID.randomUUID();
        Money reserveAmount = dollars(5000);
        
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> accountService.reserveFunds(accountId, reserveAmount));
    }

    @Test
    void shouldThrowExceptionWhenReservingInsufficientFunds() {
        UUID accountId = UUID.randomUUID();
        Account account = createAccountWithBalance(UUID.randomUUID(), Currency.USD, 3000L);
        Money reserveAmount = dollars(5000);
        
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThrows(IllegalStateException.class,
            () -> accountService.reserveFunds(accountId, reserveAmount));
    }

    @Test
    void shouldPostDebit() {
        UUID accountId = UUID.randomUUID();
        Account account = createAccountWithBalance(UUID.randomUUID(), Currency.USD, 10000L);
        Money debitAmount = dollars(3000);
        Account updatedAccount = createAccountWithBalance(account.getUserId(), Currency.USD, 7000L);
        
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);

        Account result = accountService.postDebit(accountId, debitAmount);

        assertEquals(7000L, result.getBalanceMinor());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void shouldPostCredit() {
        UUID accountId = UUID.randomUUID();
        Account account = createAccountWithBalance(UUID.randomUUID(), Currency.USD, 5000L);
        Money creditAmount = dollars(3000);
        Account updatedAccount = createAccountWithBalance(account.getUserId(), Currency.USD, 8000L);
        
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);

        Account result = accountService.postCredit(accountId, creditAmount);

        assertEquals(8000L, result.getBalanceMinor());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void shouldThrowExceptionWhenPostingToNonExistentAccount() {
        UUID accountId = UUID.randomUUID();
        Money amount = dollars(1000);
        
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> accountService.postDebit(accountId, amount));
        
        assertThrows(IllegalArgumentException.class,
            () -> accountService.postCredit(accountId, amount));
    }
}