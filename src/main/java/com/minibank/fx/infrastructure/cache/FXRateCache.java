package com.minibank.fx.infrastructure.cache;

import com.minibank.fx.domain.ExchangeRate;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface FXRateCache {
    void put(String baseCurrency, String quoteCurrency, ExchangeRate rate);
    
    void put(String baseCurrency, String quoteCurrency, ExchangeRate rate, Duration ttl);
    
    Optional<ExchangeRate> get(String baseCurrency, String quoteCurrency);
    
    void remove(String baseCurrency, String quoteCurrency);
    
    void removeByProvider(String provider);
    
    void clear();
    
    List<String> getAllCachedPairs();
    
    boolean exists(String baseCurrency, String quoteCurrency);
    
    void evictExpired();
}