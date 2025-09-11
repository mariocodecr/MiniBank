package com.minibank.accounts.adapter.persistence;

import com.minibank.accounts.domain.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountEntityMapper {
    
    public AccountEntity toEntity(Account account) {
        return new AccountEntity(
            account.getId(),
            account.getUserId(),
            account.getCurrency(),
            account.getBalanceMinor(),
            account.getStatus(),
            account.getVersion(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }
    
    public Account toDomain(AccountEntity entity) {
        return new Account(
            entity.getId(),
            entity.getUserId(),
            entity.getCurrency(),
            entity.getBalanceMinor(),
            entity.getStatus(),
            entity.getVersion(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}