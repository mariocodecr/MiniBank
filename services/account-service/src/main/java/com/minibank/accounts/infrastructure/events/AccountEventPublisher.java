package com.minibank.accounts.infrastructure.events;

import com.minibank.accounts.domain.Currency;
import com.minibank.accounts.domain.CurrencyBalance;
import com.minibank.accounts.domain.MultiCurrencyAccount;

public interface AccountEventPublisher {
    void publishAccountCreated(MultiCurrencyAccount account);
    void publishCurrencyEnabled(MultiCurrencyAccount account, Currency currency);
    void publishBalanceCredited(MultiCurrencyAccount account, Currency currency, 
                               CurrencyBalance oldBalance, CurrencyBalance newBalance, long amount);
    void publishBalanceDebited(MultiCurrencyAccount account, Currency currency, 
                              CurrencyBalance oldBalance, CurrencyBalance newBalance, long amount);
    void publishFundsReserved(MultiCurrencyAccount account, Currency currency, 
                             CurrencyBalance oldBalance, CurrencyBalance newBalance, long amount);
    void publishReservationReleased(MultiCurrencyAccount account, Currency currency, 
                                   CurrencyBalance oldBalance, CurrencyBalance newBalance, long amount);
}