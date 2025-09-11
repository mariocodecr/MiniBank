package com.minibank.accounts.adapter.persistence;

import com.minibank.accounts.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.minibank.accounts.domain.AccountTestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AccountRepositoryImpl.class, AccountEntityMapper.class})
class AccountRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("minibank_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldSaveAndFindAccount() {
        UUID userId = UUID.randomUUID();
        Account account = Account.create(userId, Currency.USD);
        
        Account savedAccount = accountRepository.save(account);
        Optional<Account> foundAccount = accountRepository.findById(savedAccount.getId());
        
        assertTrue(foundAccount.isPresent());
        assertEquals(savedAccount.getId(), foundAccount.get().getId());
        assertEquals(userId, foundAccount.get().getUserId());
        assertEquals(Currency.USD, foundAccount.get().getCurrency());
        assertEquals(0L, foundAccount.get().getBalanceMinor());
        assertEquals(AccountStatus.ACTIVE, foundAccount.get().getStatus());
    }

    @Test
    void shouldFindAccountsByUserId() {
        UUID userId = UUID.randomUUID();
        Account usdAccount = Account.create(userId, Currency.USD);
        Account crcAccount = Account.create(userId, Currency.CRC);
        
        accountRepository.save(usdAccount);
        accountRepository.save(crcAccount);
        
        List<Account> accounts = accountRepository.findByUserId(userId);
        
        assertEquals(2, accounts.size());
        assertTrue(accounts.stream().anyMatch(a -> a.getCurrency() == Currency.USD));
        assertTrue(accounts.stream().anyMatch(a -> a.getCurrency() == Currency.CRC));
    }

    @Test
    void shouldCheckExistenceByUserIdAndCurrency() {
        UUID userId = UUID.randomUUID();
        Account account = Account.create(userId, Currency.USD);
        
        accountRepository.save(account);
        
        assertTrue(accountRepository.existsByUserIdAndCurrency(userId, Currency.USD));
        assertFalse(accountRepository.existsByUserIdAndCurrency(userId, Currency.CRC));
        assertFalse(accountRepository.existsByUserIdAndCurrency(UUID.randomUUID(), Currency.USD));
    }

    @Test
    void shouldUpdateAccountWithOptimisticLocking() {
        UUID userId = UUID.randomUUID();
        Account account = Account.create(userId, Currency.USD);
        Account savedAccount = accountRepository.save(account);
        
        // Credit money to account
        savedAccount.credit(Money.of(10000L, Currency.USD));
        Account updatedAccount = accountRepository.save(savedAccount);
        
        assertEquals(10000L, updatedAccount.getBalanceMinor());
        assertTrue(updatedAccount.getVersion() > savedAccount.getVersion());
    }

    @Test
    void shouldReturnEmptyWhenAccountNotFound() {
        Optional<Account> account = accountRepository.findById(UUID.randomUUID());
        
        assertTrue(account.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoAccounts() {
        List<Account> accounts = accountRepository.findByUserId(UUID.randomUUID());
        
        assertTrue(accounts.isEmpty());
    }
}