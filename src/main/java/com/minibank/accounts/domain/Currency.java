package com.minibank.accounts.domain;

public enum Currency {
    CRC("CRC", "Costa Rican Colón"),
    USD("USD", "US Dollar");

    private final String code;
    private final String displayName;

    Currency(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}