package com.minibank.accounts.adapter.web;

import com.minibank.accounts.adapter.web.dto.AccountResponse;
import com.minibank.accounts.domain.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountResponseMapper {
    
    public AccountResponse toResponse(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getUserId(),
            account.getCurrency(),
            account.getBalance().getAmount(),
            account.getStatus(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }
}