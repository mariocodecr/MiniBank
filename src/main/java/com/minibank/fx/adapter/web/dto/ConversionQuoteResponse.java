package com.minibank.fx.adapter.web.dto;

import java.math.BigDecimal;

public class ConversionQuoteResponse {
    private final String fromCurrency;
    private final String toCurrency;
    private final long fromAmountMinor;
    private final long toAmountMinor;
    private final BigDecimal exchangeRate;

    public ConversionQuoteResponse(String fromCurrency, String toCurrency,
                                  long fromAmountMinor, long toAmountMinor,
                                  BigDecimal exchangeRate) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.fromAmountMinor = fromAmountMinor;
        this.toAmountMinor = toAmountMinor;
        this.exchangeRate = exchangeRate;
    }

    public String getFromCurrency() {
        return fromCurrency;
    }

    public String getToCurrency() {
        return toCurrency;
    }

    public long getFromAmountMinor() {
        return fromAmountMinor;
    }

    public long getToAmountMinor() {
        return toAmountMinor;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public String getCurrencyPair() {
        return fromCurrency + "/" + toCurrency;
    }
}