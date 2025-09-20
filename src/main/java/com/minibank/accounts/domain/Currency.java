package com.minibank.accounts.domain;

public enum Currency {
    CRC("CRC", "Costa Rican Colón", 2),
    USD("USD", "US Dollar", 2);

    private final String code;
    private final String displayName;
    private final int decimalPlaces;

    Currency(String code, String displayName, int decimalPlaces) {
        this.code = code;
        this.displayName = displayName;
        this.decimalPlaces = decimalPlaces;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}