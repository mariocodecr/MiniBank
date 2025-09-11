package com.minibank.accounts.adapter.persistence;

import com.minibank.accounts.domain.Account;
import com.minibank.accounts.domain.AccountRepository;
import com.minibank.accounts.domain.Currency;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AccountRepositoryImpl implements AccountRepository {
    
    private final AccountJpaRepository jpaRepository;
    private final AccountEntityMapper mapper;
    
    public AccountRepositoryImpl(AccountJpaRepository jpaRepository, AccountEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Account save(Account account) {
        AccountEntity entity = mapper.toEntity(account);
        AccountEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<Account> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    public List<Account> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
    
    @Override
    public boolean existsByUserIdAndCurrency(UUID userId, Currency currency) {
        return jpaRepository.existsByUserIdAndCurrency(userId, currency);
    }
}