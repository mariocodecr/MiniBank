package com.minibank.accounts.adapter.web;

import com.minibank.accounts.domain.Currency;
import com.minibank.accounts.domain.CurrencyBalance;
import com.minibank.accounts.domain.MultiCurrencyAccount;
import com.minibank.accounts.adapter.web.dto.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MultiCurrencyAccountMapper {

    public MultiCurrencyAccountResponse toResponse(MultiCurrencyAccount account) {
        Map<String, CurrencyBalanceResponse> balances = account.getAllBalances().entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().getCode(),
                entry -> toBalanceResponse(entry.getKey(), entry.getValue())
            ));

        return new MultiCurrencyAccountResponse(
            account.getId(),
            account.getAccountNumber(),
            account.getAccountHolderName(),
            account.getEmail(),
            account.getStatus().toString(),
            balances,
            account.getCreatedAt(),
            account.getUpdatedAt(),
            account.getVersion()
        );
    }

    public CurrencyBalanceResponse toBalanceResponse(Currency currency, CurrencyBalance balance) {
        return new CurrencyBalanceResponse(
            currency.getCode(),
            currency.getDisplayName(),
            formatAmount(balance.getTotalAmountMinor(), currency.getDecimalPlaces()),
            formatAmount(balance.getAvailableAmountMinor(), currency.getDecimalPlaces()),
            formatAmount(balance.getReservedAmountMinor(), currency.getDecimalPlaces())
        );
    }

    public SupportedCurrencyResponse toCurrencyResponse(Currency currency) {
        return new SupportedCurrencyResponse(
            currency.getCode(),
            currency.getDisplayName(),
            currency.getDecimalPlaces()
        );
    }

    private String formatAmount(long amountMinor, int decimalPlaces) {
        BigDecimal amount = BigDecimal.valueOf(amountMinor)
            .divide(BigDecimal.TEN.pow(decimalPlaces));
        return amount.toPlainString();
    }
}












