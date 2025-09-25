package com.minibank.payments.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ReserveFundsRequest {
    private UUID accountId;
    private BigDecimal amount;
    private String currencyCode;
    private String reservationId;

    public ReserveFundsRequest() {}

    public ReserveFundsRequest(UUID accountId, BigDecimal amount, String currencyCode, String reservationId) {
        this.accountId = accountId;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.reservationId = reservationId;
    }

    // Getters and setters
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
}