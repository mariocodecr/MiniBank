package com.minibank.fx.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FXConversionRepository {
    FXConversion save(FXConversion conversion);
    
    Optional<FXConversion> findById(UUID id);
    
    List<FXConversion> findByAccountId(UUID accountId);
    
    List<FXConversion> findByAccountIdAndCurrencyPair(UUID accountId, String fromCurrency, String toCurrency);
    
    List<FXConversion> findByCorrelationId(String correlationId);
    
    List<FXConversion> findConversionsAfter(Instant timestamp);
    
    List<FXConversion> findConversionsBetween(Instant startTime, Instant endTime);
}