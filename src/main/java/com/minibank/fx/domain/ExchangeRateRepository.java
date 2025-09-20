package com.minibank.fx.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateRepository {
    ExchangeRate save(ExchangeRate exchangeRate);
    
    Optional<ExchangeRate> findById(UUID id);
    
    Optional<ExchangeRate> findLatestRate(String baseCurrency, String quoteCurrency);
    
    List<ExchangeRate> findLatestRatesByProvider(String provider);
    
    List<ExchangeRate> findRatesAfter(Instant timestamp);
    
    List<ExchangeRate> findExpiredRates();
    
    void deleteExpiredRates(Instant before);
    
    boolean existsValidRate(String baseCurrency, String quoteCurrency);
}