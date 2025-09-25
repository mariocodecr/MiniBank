package com.minibank.accounts.adapter.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.minibank.accounts.domain.Currency;
import com.minibank.accounts.domain.CurrencyBalance;
import com.minibank.accounts.domain.MultiCurrencyAccount;
import com.minibank.accounts.domain.MultiCurrencyAccountRepository;

@Repository
@Transactional
public class MultiCurrencyAccountRepositoryImpl implements MultiCurrencyAccountRepository {

    private final AccountJpaRepository accountJpaRepository;
    private final AccountCurrencyBalanceJpaRepository balanceJpaRepository;
    private final SupportedCurrencyJpaRepository currencyJpaRepository;
    private final MultiCurrencyAccountMapper accountMapper;

    public MultiCurrencyAccountRepositoryImpl(AccountJpaRepository accountJpaRepository,
                                            AccountCurrencyBalanceJpaRepository balanceJpaRepository,
                                            SupportedCurrencyJpaRepository currencyJpaRepository,
                                            MultiCurrencyAccountMapper accountMapper) {
        this.accountJpaRepository = accountJpaRepository;
        this.balanceJpaRepository = balanceJpaRepository;
        this.currencyJpaRepository = currencyJpaRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    public MultiCurrencyAccount save(MultiCurrencyAccount account) {
        // Save account entity
        AccountEntity accountEntity = accountMapper.toAccountEntity(account);
        AccountEntity savedAccountEntity = accountJpaRepository.save(accountEntity);
        
        // Get existing balances to determine what changed
        List<AccountCurrencyBalanceEntity> existingBalances = 
            balanceJpaRepository.findByAccountIdOrderByCurrency(account.getId());
        
        Map<String, AccountCurrencyBalanceEntity> existingBalanceMap = existingBalances.stream()
            .collect(Collectors.toMap(AccountCurrencyBalanceEntity::getCurrencyCode, b -> b));
        
        // Save/update currency balances
        Map<Currency, CurrencyBalance> accountBalances = account.getAllBalances();
        List<AccountCurrencyBalanceEntity> balanceEntitiesToSave = new ArrayList<>();
        
        for (Map.Entry<Currency, CurrencyBalance> entry : accountBalances.entrySet()) {
            Currency currency = entry.getKey();
            CurrencyBalance balance = entry.getValue();
            
            AccountCurrencyBalanceEntity balanceEntity = existingBalanceMap.get(currency.getCode());
            
            if (balanceEntity == null) {
                // Create new balance
                balanceEntity = accountMapper.toBalanceEntity(account.getId(), balance);
            } else {
                // Update existing balance
                accountMapper.updateBalanceEntity(balanceEntity, balance);
            }
            
            balanceEntitiesToSave.add(balanceEntity);
        }
        
        balanceJpaRepository.saveAll(balanceEntitiesToSave);
        
        // Remove balances that are no longer present
        Set<String> currentCurrencies = accountBalances.keySet().stream()
            .map(Currency::getCode)
            .collect(Collectors.toSet());
        
        List<AccountCurrencyBalanceEntity> balancesToRemove = existingBalances.stream()
            .filter(b -> !currentCurrencies.contains(b.getCurrencyCode()))
            .collect(Collectors.toList());
        
        if (!balancesToRemove.isEmpty()) {
            balanceJpaRepository.deleteAll(balancesToRemove);
        }
        
        return findById(account.getId()).orElseThrow(() -> 
            new IllegalStateException("Account not found after save: " + account.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MultiCurrencyAccount> findById(UUID id) {
        Optional<AccountEntity> accountEntity = accountJpaRepository.findById(id);
        if (accountEntity.isEmpty()) {
            return Optional.empty();
        }
        
        List<AccountCurrencyBalanceEntity> balanceEntities = 
            balanceJpaRepository.findByAccountIdOrderByCurrency(id);
        
        Map<String, SupportedCurrencyEntity> supportedCurrencies = getSupportedCurrenciesMap();
        
        return Optional.of(accountMapper.toDomainObject(accountEntity.get(), balanceEntities, supportedCurrencies));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MultiCurrencyAccount> findByAccountNumber(String accountNumber) {
        Optional<AccountEntity> accountEntity = accountJpaRepository.findByAccountNumber(accountNumber);
        if (accountEntity.isEmpty()) {
            return Optional.empty();
        }
        
        List<AccountCurrencyBalanceEntity> balanceEntities = 
            balanceJpaRepository.findByAccountIdOrderByCurrency(accountEntity.get().getId());
        
        Map<String, SupportedCurrencyEntity> supportedCurrencies = getSupportedCurrenciesMap();
        
        return Optional.of(accountMapper.toDomainObject(accountEntity.get(), balanceEntities, supportedCurrencies));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MultiCurrencyAccount> findByEmail(String email) {
        Optional<AccountEntity> accountEntity = accountJpaRepository.findByEmail(email);
        if (accountEntity.isEmpty()) {
            return Optional.empty();
        }
        
        List<AccountCurrencyBalanceEntity> balanceEntities = 
            balanceJpaRepository.findByAccountIdOrderByCurrency(accountEntity.get().getId());
        
        Map<String, SupportedCurrencyEntity> supportedCurrencies = getSupportedCurrenciesMap();
        
        return Optional.of(accountMapper.toDomainObject(accountEntity.get(), balanceEntities, supportedCurrencies));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MultiCurrencyAccount> findByCurrencySupported(Currency currency) {
        // Find all accounts that have balance entries for this currency
        List<AccountCurrencyBalanceEntity> balanceEntities =
            balanceJpaRepository.findAll().stream()
                .filter(b -> currency.getCode().equals(b.getCurrencyCode()))
                .collect(Collectors.toList());
        
        Set<UUID> accountIds = balanceEntities.stream()
            .map(AccountCurrencyBalanceEntity::getAccountId)
            .collect(Collectors.toSet());
        
        List<AccountEntity> accountEntities = accountJpaRepository.findAllById(accountIds);
        Map<String, SupportedCurrencyEntity> supportedCurrencies = getSupportedCurrenciesMap();
        
        return accountEntities.stream()
            .map(accountEntity -> {
                List<AccountCurrencyBalanceEntity> accountBalances = balanceEntities.stream()
                    .filter(b -> b.getAccountId().equals(accountEntity.getId()))
                    .collect(Collectors.toList());
                return accountMapper.toDomainObject(accountEntity, accountBalances, supportedCurrencies);
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MultiCurrencyAccount> findAll() {
        List<AccountEntity> accountEntities = accountJpaRepository.findAll();
        Map<String, SupportedCurrencyEntity> supportedCurrencies = getSupportedCurrenciesMap();
        
        return accountEntities.stream()
            .map(accountEntity -> {
                List<AccountCurrencyBalanceEntity> balanceEntities = 
                    balanceJpaRepository.findByAccountIdOrderByCurrency(accountEntity.getId());
                return accountMapper.toDomainObject(accountEntity, balanceEntities, supportedCurrencies);
            })
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        balanceJpaRepository.deleteByAccountId(id);
        accountJpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return accountJpaRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAccountNumber(String accountNumber) {
        return accountJpaRepository.existsByAccountNumber(accountNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return accountJpaRepository.existsByEmail(email);
    }

    private Map<String, SupportedCurrencyEntity> getSupportedCurrenciesMap() {
        return currencyJpaRepository.findAll().stream()
            .collect(Collectors.toMap(SupportedCurrencyEntity::getCurrencyCode, c -> c));
    }
}