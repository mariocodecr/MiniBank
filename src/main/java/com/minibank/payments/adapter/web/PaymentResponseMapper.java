package com.minibank.payments.adapter.web;

import com.minibank.payments.adapter.web.dto.PaymentResponse;
import com.minibank.payments.domain.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentResponseMapper {
    
    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getRequestId(),
            payment.getFromAccountId(),
            payment.getToAccountId(),
            payment.getAmountMinor(),
            payment.getCurrency(),
            payment.getStatus(),
            payment.getFailureReason(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}