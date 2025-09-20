package com.minibank.fx.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FXRateProvider {
    String getProviderName();
    
    int getPriority();
    
    boolean isEnabled();
    
    Optional<ExchangeRate> getRate(String baseCurrency, String quoteCurrency);
    
    List<ExchangeRate> getAllRates();
    
    boolean supports(String baseCurrency, String quoteCurrency);
    
    Instant getLastUpdate();
    
    ProviderStatus getStatus();
    
    enum ProviderStatus {
        ACTIVE, INACTIVE, ERROR, MAINTENANCE
    }
}