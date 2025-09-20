package com.minibank.accounts.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SupportedCurrencyResponse {
    
    @NotBlank
    private String code;
    
    @NotBlank
    private String name;
    
    @NotNull
    private int decimalPlaces;

    public SupportedCurrencyResponse() {}

    public SupportedCurrencyResponse(String code, String name, int decimalPlaces) {
        this.code = code;
        this.name = name;
        this.decimalPlaces = decimalPlaces;
    }

    // Getters and Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDecimalPlaces() { return decimalPlaces; }
    public void setDecimalPlaces(int decimalPlaces) { this.decimalPlaces = decimalPlaces; }
}









