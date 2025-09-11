package com.minibank.accounts.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(UUID id);
    List<Account> findByUserId(UUID userId);
    boolean existsByUserIdAndCurrency(UUID userId, Currency currency);
}