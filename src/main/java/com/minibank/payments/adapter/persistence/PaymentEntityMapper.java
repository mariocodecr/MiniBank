package com.minibank.payments.adapter.persistence;

import com.minibank.payments.domain.Payment;
import com.minibank.accounts.domain.Currency;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.minibank.accounts.adapter.persistence.AccountJpaRepository;

@Component
public class PaymentEntityMapper {

    private final AccountJpaRepository accountRepository;

    @Autowired
    public PaymentEntityMapper(AccountJpaRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public PaymentEntity toEntity(Payment payment) {
        return new PaymentEntity(
            payment.getId(),
            payment.getRequestId(),
            payment.getFromAccountId(),
            payment.getToAccountId(),
            payment.getAmountMinor(),
            payment.getStatus(),
            payment.getFailureReason(),
            payment.getVersion(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }

    public Payment toDomain(PaymentEntity entity, Currency currency) {
        return new Payment(
            entity.getId(),
            entity.getRequestId(),
            entity.getFromAccountId(),
            entity.getToAccountId(),
            entity.getAmountMinor(),
            currency,
            entity.getStatus(),
            entity.getFailureReason(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }

    public Payment toDomain(PaymentEntity entity) {
        // Get currency from the from account
        Currency currency = accountRepository.findById(entity.getFromAccountId())
            .map(account -> account.getCurrency())
            .orElse(Currency.USD); // fallback currency

        return toDomain(entity, currency);
    }
}