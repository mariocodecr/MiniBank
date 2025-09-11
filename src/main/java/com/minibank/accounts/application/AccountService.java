package com.minibank.accounts.application;

import com.minibank.accounts.domain.Account;
import com.minibank.accounts.domain.AccountRepository;
import com.minibank.accounts.domain.Currency;
import com.minibank.accounts.domain.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AccountService {
    
    private final AccountRepository accountRepository;
    
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    
    public Account createAccount(UUID userId, Currency currency) {
        if (accountRepository.existsByUserIdAndCurrency(userId, currency)) {
            throw new IllegalArgumentException(
                "User already has an account in currency: " + currency);
        }
        
        Account account = Account.create(userId, currency);
        return accountRepository.save(account);
    }
    
    @Transactional(readOnly = true)
    public Optional<Account> getAccount(UUID accountId) {
        return accountRepository.findById(accountId);
    }
    
    @Transactional(readOnly = true)
    public List<Account> getAccountsByUserId(UUID userId) {
        return accountRepository.findByUserId(userId);
    }
    
    public Account reserveFunds(UUID accountId, Money amount) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        if (!account.canDebit(amount)) {
            throw new IllegalStateException("Insufficient funds or account inactive");
        }
        
        account.debit(amount);
        return accountRepository.save(account);
    }
    
    public Account postDebit(UUID accountId, Money amount) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        account.debit(amount);
        return accountRepository.save(account);
    }
    
    public Account postCredit(UUID accountId, Money amount) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        
        account.credit(amount);
        return accountRepository.save(account);
    }
}