package com.minibank.payments.adapter.persistence;

import com.minibank.payments.domain.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentEntityMapper {
    
    public PaymentEntity toEntity(Payment payment) {
        return new PaymentEntity(
            payment.getId(),
            payment.getRequestId(),
            payment.getFromAccountId(),
            payment.getToAccountId(),
            payment.getAmountMinor(),
            payment.getCurrency(),
            payment.getStatus(),
            payment.getFailureReason(),
            payment.getVersion(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
    
    public Payment toDomain(PaymentEntity entity) {
        return new Payment(
            entity.getId(),
            entity.getRequestId(),
            entity.getFromAccountId(),
            entity.getToAccountId(),
            entity.getAmountMinor(),
            entity.getCurrency(),
            entity.getStatus(),
            entity.getFailureReason(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }
}