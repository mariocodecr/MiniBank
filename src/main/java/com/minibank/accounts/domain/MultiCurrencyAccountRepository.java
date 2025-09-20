package com.minibank.accounts.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MultiCurrencyAccountRepository {
    MultiCurrencyAccount save(MultiCurrencyAccount account);
    Optional<MultiCurrencyAccount> findById(UUID id);
    Optional<MultiCurrencyAccount> findByAccountNumber(String accountNumber);
    Optional<MultiCurrencyAccount> findByEmail(String email);
    List<MultiCurrencyAccount> findByCurrencySupported(Currency currency);
    List<MultiCurrencyAccount> findAll();
    void deleteById(UUID id);
    boolean existsById(UUID id);
    boolean existsByAccountNumber(String accountNumber);
    boolean existsByEmail(String email);
}