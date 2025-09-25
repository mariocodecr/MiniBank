package com.minibank.accounts.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public class ReserveFundsRequest {
    @NotNull
    private UUID accountId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private String currencyCode;

    @NotNull
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