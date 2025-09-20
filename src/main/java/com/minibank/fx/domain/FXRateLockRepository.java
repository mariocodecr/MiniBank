package com.minibank.fx.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FXRateLockRepository {
    FXRateLock save(FXRateLock rateLock);
    
    Optional<FXRateLock> findById(UUID id);
    
    List<FXRateLock> findActiveByAccountId(UUID accountId);
    
    List<FXRateLock> findByCorrelationId(String correlationId);
    
    List<FXRateLock> findExpiredActiveLocks();
    
    boolean existsActiveLock(String baseCurrency, String quoteCurrency, UUID accountId);
    
    int deleteOldLocks(Instant before);
}