package com.minibank.payments.infrastructure.saga;

import com.minibank.payments.infrastructure.client.AccountServiceClient;
import com.minibank.payments.infrastructure.events.PaymentEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentSaga {

    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentSaga(AccountServiceClient accountServiceClient,
                      KafkaTemplate<String, Object> kafkaTemplate) {
        this.accountServiceClient = accountServiceClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "payment.requested", groupId = "payment-saga")
    @Transactional
    public void handlePaymentRequested(PaymentRequestedEvent event) {
        try {
            // Step 1: Reserve funds from source account
            var reserveResult = accountServiceClient.reserveFunds(
                event.getFromAccountId(),
                event.getAmount(),
                event.getCurrencyCode()
            );

            if (reserveResult.isSuccess()) {
                // Publish funds reserved event
                kafkaTemplate.send("payment.funds-reserved",
                    new FundsReservedEvent(
                        event.getPaymentId(),
                        event.getFromAccountId(),
                        event.getToAccountId(),
                        event.getAmount(),
                        event.getCurrencyCode()
                    )
                );
            } else {
                // Publish payment failed event
                kafkaTemplate.send("payment.failed",
                    new PaymentFailedEvent(
                        event.getPaymentId(),
                        "INSUFFICIENT_FUNDS",
                        reserveResult.getErrorMessage()
                    )
                );
            }

        } catch (Exception e) {
            // Publish payment failed event
            kafkaTemplate.send("payment.failed",
                new PaymentFailedEvent(
                    event.getPaymentId(),
                    "SYSTEM_ERROR",
                    "Error reserving funds: " + e.getMessage()
                )
            );
        }
    }

    @KafkaListener(topics = "payment.funds-reserved", groupId = "payment-saga")
    @Transactional
    public void handleFundsReserved(FundsReservedEvent event) {
        try {
            // Step 2: Debit from source account
            var debitResult = accountServiceClient.postDebit(
                event.getFromAccountId(),
                event.getAmount(),
                event.getCurrencyCode()
            );

            if (debitResult.isSuccess()) {
                // Step 3: Credit to destination account
                var creditResult = accountServiceClient.postCredit(
                    event.getToAccountId(),
                    event.getAmount(),
                    event.getCurrencyCode()
                );

                if (creditResult.isSuccess()) {
                    // Publish payment completed event
                    kafkaTemplate.send("payment.completed",
                        new PaymentCompletedEvent(
                            event.getPaymentId(),
                            event.getFromAccountId(),
                            event.getToAccountId(),
                            event.getAmount(),
                            event.getCurrencyCode()
                        )
                    );
                } else {
                    // Compensate: credit back to source account
                    accountServiceClient.postCredit(
                        event.getFromAccountId(),
                        event.getAmount(),
                        event.getCurrencyCode()
                    );

                    kafkaTemplate.send("payment.failed",
                        new PaymentFailedEvent(
                            event.getPaymentId(),
                            "CREDIT_FAILED",
                            creditResult.getErrorMessage()
                        )
                    );
                }
            } else {
                // Release reserved funds (compensation)
                // This would need a separate API endpoint
                kafkaTemplate.send("payment.failed",
                    new PaymentFailedEvent(
                        event.getPaymentId(),
                        "DEBIT_FAILED",
                        debitResult.getErrorMessage()
                    )
                );
            }

        } catch (Exception e) {
            // Compensation logic would go here
            kafkaTemplate.send("payment.failed",
                new PaymentFailedEvent(
                    event.getPaymentId(),
                    "SYSTEM_ERROR",
                    "Error processing payment: " + e.getMessage()
                )
            );
        }
    }

    // Event classes
    public static class PaymentRequestedEvent {
        private UUID paymentId;
        private UUID fromAccountId;
        private UUID toAccountId;
        private BigDecimal amount;
        private String currencyCode;

        // Constructors, getters, setters
        public PaymentRequestedEvent() {}

        public PaymentRequestedEvent(UUID paymentId, UUID fromAccountId, UUID toAccountId,
                                   BigDecimal amount, String currencyCode) {
            this.paymentId = paymentId;
            this.fromAccountId = fromAccountId;
            this.toAccountId = toAccountId;
            this.amount = amount;
            this.currencyCode = currencyCode;
        }

        public UUID getPaymentId() { return paymentId; }
        public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

        public UUID getFromAccountId() { return fromAccountId; }
        public void setFromAccountId(UUID fromAccountId) { this.fromAccountId = fromAccountId; }

        public UUID getToAccountId() { return toAccountId; }
        public void setToAccountId(UUID toAccountId) { this.toAccountId = toAccountId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    }

    public static class FundsReservedEvent {
        private UUID paymentId;
        private UUID fromAccountId;
        private UUID toAccountId;
        private BigDecimal amount;
        private String currencyCode;

        public FundsReservedEvent() {}

        public FundsReservedEvent(UUID paymentId, UUID fromAccountId, UUID toAccountId,
                                BigDecimal amount, String currencyCode) {
            this.paymentId = paymentId;
            this.fromAccountId = fromAccountId;
            this.toAccountId = toAccountId;
            this.amount = amount;
            this.currencyCode = currencyCode;
        }

        public UUID getPaymentId() { return paymentId; }
        public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

        public UUID getFromAccountId() { return fromAccountId; }
        public void setFromAccountId(UUID fromAccountId) { this.fromAccountId = fromAccountId; }

        public UUID getToAccountId() { return toAccountId; }
        public void setToAccountId(UUID toAccountId) { this.toAccountId = toAccountId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    }

    public static class PaymentCompletedEvent {
        private UUID paymentId;
        private UUID fromAccountId;
        private UUID toAccountId;
        private BigDecimal amount;
        private String currencyCode;

        public PaymentCompletedEvent() {}

        public PaymentCompletedEvent(UUID paymentId, UUID fromAccountId, UUID toAccountId,
                                   BigDecimal amount, String currencyCode) {
            this.paymentId = paymentId;
            this.fromAccountId = fromAccountId;
            this.toAccountId = toAccountId;
            this.amount = amount;
            this.currencyCode = currencyCode;
        }

        public UUID getPaymentId() { return paymentId; }
        public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

        public UUID getFromAccountId() { return fromAccountId; }
        public void setFromAccountId(UUID fromAccountId) { this.fromAccountId = fromAccountId; }

        public UUID getToAccountId() { return toAccountId; }
        public void setToAccountId(UUID toAccountId) { this.toAccountId = toAccountId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    }

    public static class PaymentFailedEvent {
        private UUID paymentId;
        private String failureReason;
        private String errorMessage;

        public PaymentFailedEvent() {}

        public PaymentFailedEvent(UUID paymentId, String failureReason, String errorMessage) {
            this.paymentId = paymentId;
            this.failureReason = failureReason;
            this.errorMessage = errorMessage;
        }

        public UUID getPaymentId() { return paymentId; }
        public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}