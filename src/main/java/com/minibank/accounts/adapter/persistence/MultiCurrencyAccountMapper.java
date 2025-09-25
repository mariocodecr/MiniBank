package com.minibank.accounts.adapter.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.minibank.accounts.domain.Currency;
import com.minibank.accounts.domain.CurrencyBalance;
import com.minibank.accounts.domain.MultiCurrencyAccount;

@Component
public class MultiCurrencyAccountMapper {

    public MultiCurrencyAccount toDomainObject(AccountEntity accountEntity, 
                                              List<AccountCurrencyBalanceEntity> balanceEntities,
                                              Map<String, SupportedCurrencyEntity> supportedCurrencies) {
        if (accountEntity == null) {
            return null;
        }

        Map<Currency, CurrencyBalance> currencyBalances = new HashMap<>();
        
        for (AccountCurrencyBalanceEntity balanceEntity : balanceEntities) {
            SupportedCurrencyEntity currencyEntity = supportedCurrencies.get(balanceEntity.getCurrencyCode());
            if (currencyEntity != null && currencyEntity.getIsActive()) {
                Currency currency = toCurrency(currencyEntity);
                CurrencyBalance balance = toCurrencyBalance(balanceEntity, currency);
                currencyBalances.put(currency, balance);
            }
        }

        return new MultiCurrencyAccount(
            accountEntity.getId(),
            accountEntity.getId().toString(), // Use ID as account number
            "Account Holder", // Default account holder name
            "user@example.com", // Default email
            accountEntity.getStatus(),
            currencyBalances,
            accountEntity.getCreatedAt(),
            accountEntity.getUpdatedAt(),
            accountEntity.getVersion().intValue()
        );
    }

    public AccountEntity toAccountEntity(MultiCurrencyAccount account) {
        if (account == null) {
            return null;
        }

        AccountEntity entity = new AccountEntity();
        entity.setId(account.getId());
        entity.setStatus(account.getStatus());
        entity.setCreatedAt(account.getCreatedAt());
        entity.setUpdatedAt(account.getUpdatedAt());
        entity.setVersion((long) account.getVersion());
        
        return entity;
    }

    public AccountCurrencyBalanceEntity toBalanceEntity(UUID accountId, CurrencyBalance balance) {
        if (balance == null) {
            return null;
        }

        AccountCurrencyBalanceEntity entity = new AccountCurrencyBalanceEntity();
        entity.setAccountId(accountId);
        entity.setCurrencyCode(balance.getCurrency().getCode());
        entity.setAvailableAmountMinor(balance.getAvailableAmountMinor());
        entity.setReservedAmountMinor(balance.getReservedAmountMinor());
        
        return entity;
    }

    public void updateBalanceEntity(AccountCurrencyBalanceEntity entity, CurrencyBalance balance) {
        if (entity == null || balance == null) {
            return;
        }

        entity.setAvailableAmountMinor(balance.getAvailableAmountMinor());
        entity.setReservedAmountMinor(balance.getReservedAmountMinor());
    }

    public Currency toCurrency(SupportedCurrencyEntity currencyEntity) {
        if (currencyEntity == null) {
            return null;
        }

        return Currency.valueOf(currencyEntity.getCurrencyCode());
    }

    public CurrencyBalance toCurrencyBalance(AccountCurrencyBalanceEntity balanceEntity, Currency currency) {
        if (balanceEntity == null || currency == null) {
            return null;
        }

        return new CurrencyBalance(
            currency,
            balanceEntity.getAvailableAmountMinor(),
            balanceEntity.getReservedAmountMinor(),
            balanceEntity.getVersion()
        );
    }

    public SupportedCurrencyEntity toSupportedCurrencyEntity(Currency currency, boolean isActive) {
        if (currency == null) {
            return null;
        }

        return new SupportedCurrencyEntity(
            currency.getCode(),
            currency.getDisplayName(),
            2, // Default decimal places
            1L, // Default minimum amount
            currency.getCode(), // Use code as symbol
            isActive
        );
    }
}