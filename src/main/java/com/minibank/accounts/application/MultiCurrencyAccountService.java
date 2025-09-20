package com.minibank.accounts.application;

import com.minibank.accounts.domain.*;
import com.minibank.accounts.infrastructure.events.AccountEventPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MultiCurrencyAccountService {
    private static final Logger logger = LoggerFactory.getLogger(MultiCurrencyAccountService.class);

    private final MultiCurrencyAccountRepository accountRepository;
    private final CurrencyService currencyService;
    private final AccountEventPublisher eventPublisher;
    
    // Metrics
    private final Counter accountsCreated;
    private final Counter currenciesEnabled;
    private final Counter balanceOperations;
    private final Timer operationLatency;

    public MultiCurrencyAccountService(MultiCurrencyAccountRepository accountRepository,
                                     CurrencyService currencyService,
                                     AccountEventPublisher eventPublisher,
                                     MeterRegistry meterRegistry) {
        this.accountRepository = accountRepository;
        this.currencyService = currencyService;
        this.eventPublisher = eventPublisher;
        
        // Initialize metrics
        this.accountsCreated = Counter.builder("accounts.created.total")
            .description("Total number of accounts created")
            .register(meterRegistry);
        this.currenciesEnabled = Counter.builder("accounts.currencies.enabled.total")
            .description("Total number of currencies enabled for accounts")
            .register(meterRegistry);
        this.balanceOperations = Counter.builder("accounts.balance.operations.total")
            .description("Total number of balance operations")
            .register(meterRegistry);
        this.operationLatency = Timer.builder("accounts.operation.duration.seconds")
            .description("Account operation latency")
            .register(meterRegistry);
    }

    public MultiCurrencyAccount createAccount(String accountHolderName, String email) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.info("Creating new multi-currency account for: {}", email);
            
            // Validate email uniqueness
            if (accountRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Account with email already exists: " + email);
            }
            
            // Generate unique account number
            String accountNumber = generateAccountNumber();
            
            // Create account with USD as default currency
            MultiCurrencyAccount account = MultiCurrencyAccount.create(accountNumber, accountHolderName, email);
            Currency usd = currencyService.getCurrency("USD")
                .orElseThrow(() -> new IllegalStateException("USD currency not supported"));
            
            account = account.enableCurrency(usd);
            MultiCurrencyAccount savedAccount = accountRepository.save(account);
            
            // Publish account created event
            eventPublisher.publishAccountCreated(savedAccount);
            
            accountsCreated.increment();
            logger.info("Created multi-currency account: {}", savedAccount.getId());
            
            return savedAccount;
            
        } finally {
            sample.stop(operationLatency);
        }
    }

    public MultiCurrencyAccount enableCurrency(UUID accountId, String currencyCode) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.info("Enabling currency {} for account {}", currencyCode, accountId);
            
            MultiCurrencyAccount account = getAccount(accountId);
            Currency currency = currencyService.getCurrency(currencyCode)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported currency: " + currencyCode));
            
            if (account.hasCurrency(currency)) {
                logger.debug("Currency {} already enabled for account {}", currencyCode, accountId);
                return account;
            }
            
            MultiCurrencyAccount updatedAccount = account.enableCurrency(currency);
            updatedAccount = accountRepository.save(updatedAccount);
            
            // Publish currency enabled event
            eventPublisher.publishCurrencyEnabled(updatedAccount, currency);
            
            currenciesEnabled.increment();
            logger.info("Enabled currency {} for account {}", currencyCode, accountId);
            
            return updatedAccount;
            
        } finally {
            sample.stop(operationLatency);
        }
    }

    public MultiCurrencyAccount credit(UUID accountId, String currencyCode, long amountMinor) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.info("Crediting {} {} minor units to account {}", amountMinor, currencyCode, accountId);
            
            MultiCurrencyAccount account = getAccount(accountId);
            Currency currency = currencyService.getCurrency(currencyCode)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported currency: " + currencyCode));
            
            // Enable currency if not already enabled
            if (!account.hasCurrency(currency)) {
                account = account.enableCurrency(currency);
            }
            
            CurrencyBalance oldBalance = account.getBalance(currency);
            MultiCurrencyAccount updatedAccount = account.credit(currency, amountMinor);
            updatedAccount = accountRepository.save(updatedAccount);
            
            // Publish balance credited event
            CurrencyBalance newBalance = updatedAccount.getBalance(currency);
            eventPublisher.publishBalanceCredited(updatedAccount, currency, oldBalance, newBalance, amountMinor);
            
            balanceOperations.increment();
            logger.info("Credited {} {} minor units to account {}", amountMinor, currencyCode, accountId);
            
            return updatedAccount;
            
        } finally {
            sample.stop(operationLatency);
        }
    }

    public MultiCurrencyAccount debit(UUID accountId, String currencyCode, long amountMinor) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.info("Debiting {} {} minor units from account {}", amountMinor, currencyCode, accountId);
            
            MultiCurrencyAccount account = getAccount(accountId);
            Currency currency = currencyService.getCurrency(currencyCode)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported currency: " + currencyCode));
            
            CurrencyBalance oldBalance = account.getBalance(currency);
            MultiCurrencyAccount updatedAccount = account.debit(currency, amountMinor);
            updatedAccount = accountRepository.save(updatedAccount);
            
            // Publish balance debited event
            CurrencyBalance newBalance = updatedAccount.getBalance(currency);
            eventPublisher.publishBalanceDebited(updatedAccount, currency, oldBalance, newBalance, amountMinor);
            
            balanceOperations.increment();
            logger.info("Debited {} {} minor units from account {}", amountMinor, currencyCode, accountId);
            
            return updatedAccount;
            
        } finally {
            sample.stop(operationLatency);
        }
    }

    public MultiCurrencyAccount reserveFunds(UUID accountId, String currencyCode, long amountMinor) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.info("Reserving {} {} minor units for account {}", amountMinor, currencyCode, accountId);
            
            MultiCurrencyAccount account = getAccount(accountId);
            Currency currency = currencyService.getCurrency(currencyCode)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported currency: " + currencyCode));
            
            CurrencyBalance oldBalance = account.getBalance(currency);
            MultiCurrencyAccount updatedAccount = account.reserve(currency, amountMinor);
            updatedAccount = accountRepository.save(updatedAccount);
            
            // Publish funds reserved event
            CurrencyBalance newBalance = updatedAccount.getBalance(currency);
            eventPublisher.publishFundsReserved(updatedAccount, currency, oldBalance, newBalance, amountMinor);
            
            balanceOperations.increment();
            logger.info("Reserved {} {} minor units for account {}", amountMinor, currencyCode, accountId);
            
            return updatedAccount;
            
        } finally {
            sample.stop(operationLatency);
        }
    }

    public MultiCurrencyAccount releaseReservation(UUID accountId, String currencyCode, long amountMinor) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.info("Releasing {} {} minor units reservation for account {}", amountMinor, currencyCode, accountId);
            
            MultiCurrencyAccount account = getAccount(accountId);
            Currency currency = currencyService.getCurrency(currencyCode)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported currency: " + currencyCode));
            
            CurrencyBalance oldBalance = account.getBalance(currency);
            MultiCurrencyAccount updatedAccount = account.releaseReservation(currency, amountMinor);
            updatedAccount = accountRepository.save(updatedAccount);
            
            // Publish reservation released event
            CurrencyBalance newBalance = updatedAccount.getBalance(currency);
            eventPublisher.publishReservationReleased(updatedAccount, currency, oldBalance, newBalance, amountMinor);
            
            balanceOperations.increment();
            logger.info("Released {} {} minor units reservation for account {}", amountMinor, currencyCode, accountId);
            
            return updatedAccount;
            
        } finally {
            sample.stop(operationLatency);
        }
    }

    @Transactional(readOnly = true)
    public Optional<MultiCurrencyAccount> findById(UUID accountId) {
        return accountRepository.findById(accountId);
    }

    @Transactional(readOnly = true)
    public Optional<MultiCurrencyAccount> findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    @Transactional(readOnly = true)
    public Optional<MultiCurrencyAccount> findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Map<Currency, CurrencyBalance> getAccountBalances(UUID accountId) {
        MultiCurrencyAccount account = getAccount(accountId);
        return account.getAllBalances();
    }

    @Transactional(readOnly = true)
    public CurrencyBalance getBalance(UUID accountId, String currencyCode) {
        MultiCurrencyAccount account = getAccount(accountId);
        Currency currency = currencyService.getCurrency(currencyCode)
            .orElseThrow(() -> new IllegalArgumentException("Unsupported currency: " + currencyCode));
        
        return account.getBalance(currency);
    }

    @Transactional(readOnly = true)
    public List<MultiCurrencyAccount> findAccountsWithCurrency(String currencyCode) {
        Currency currency = currencyService.getCurrency(currencyCode)
            .orElseThrow(() -> new IllegalArgumentException("Unsupported currency: " + currencyCode));
        
        return accountRepository.findByCurrencySupported(currency);
    }

    private MultiCurrencyAccount getAccount(UUID accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }

    private String generateAccountNumber() {
        // Simple account number generation - in production, use more sophisticated approach
        return "ACC" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }
}