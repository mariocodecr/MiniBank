package com.minibank.fx.application;

import com.minibank.fx.domain.ExchangeRate;
import com.minibank.fx.domain.FXRateLock;
import com.minibank.fx.domain.FXRateLockRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class FXRateLockService {
    private static final Logger logger = LoggerFactory.getLogger(FXRateLockService.class);

    private final FXRateService fxRateService;
    private final FXRateLockRepository rateLockRepository;
    
    @Value("${fx.rate-lock.default-duration-minutes:5}")
    private int defaultLockDurationMinutes;
    
    @Value("${fx.rate-lock.max-duration-minutes:30}")
    private int maxLockDurationMinutes;

    // Metrics
    private final Counter locksCreated;
    private final Counter locksUsed;
    private final Counter locksExpired;
    private final Timer lockAcquisitionLatency;

    public FXRateLockService(FXRateService fxRateService, FXRateLockRepository rateLockRepository,
                            MeterRegistry meterRegistry) {
        this.fxRateService = fxRateService;
        this.rateLockRepository = rateLockRepository;
        
        // Initialize metrics
        this.locksCreated = Counter.builder("fx.rate.locks.created.total")
            .description("Total number of FX rate locks created")
            .register(meterRegistry);
        this.locksUsed = Counter.builder("fx.rate.locks.used.total")
            .description("Total number of FX rate locks used")
            .register(meterRegistry);
        this.locksExpired = Counter.builder("fx.rate.locks.expired.total")
            .description("Total number of FX rate locks expired")
            .register(meterRegistry);
        this.lockAcquisitionLatency = Timer.builder("fx.rate.lock.acquisition.duration")
            .description("FX rate lock acquisition latency")
            .register(meterRegistry);
    }

    public Optional<FXRateLock> lockExchangeRate(String baseCurrency, String quoteCurrency,
                                                UUID accountId, String correlationId) {
        return lockExchangeRate(baseCurrency, quoteCurrency, accountId, correlationId, defaultLockDurationMinutes);
    }

    public Optional<FXRateLock> lockExchangeRate(String baseCurrency, String quoteCurrency,
                                                UUID accountId, String correlationId, int durationMinutes) {
        Timer.Sample sample = Timer.start();
        
        try {
            logger.info("Locking FX rate for {}/{} for account {} with duration {} minutes",
                       baseCurrency, quoteCurrency, accountId, durationMinutes);

            if (durationMinutes > maxLockDurationMinutes) {
                throw new IllegalArgumentException("Lock duration cannot exceed " + maxLockDurationMinutes + " minutes");
            }

            Optional<ExchangeRate> exchangeRate = fxRateService.getExchangeRate(baseCurrency, quoteCurrency);
            if (exchangeRate.isEmpty()) {
                logger.warn("No exchange rate available for {}/{}", baseCurrency, quoteCurrency);
                return Optional.empty();
            }

            FXRateLock rateLock = FXRateLock.create(baseCurrency, quoteCurrency, exchangeRate.get(),
                                                   accountId, correlationId, durationMinutes);
            
            FXRateLock savedLock = rateLockRepository.save(rateLock);
            
            locksCreated.increment();
            logger.info("FX rate locked: {} for account {} expires at {}",
                       savedLock.getCurrencyPair(), accountId, savedLock.getExpiresAt());
            
            return Optional.of(savedLock);
            
        } catch (Exception e) {
            logger.error("Failed to lock FX rate for {}/{} for account {}: {}",
                        baseCurrency, quoteCurrency, accountId, e.getMessage(), e);
            return Optional.empty();
        } finally {
            sample.stop(lockAcquisitionLatency);
        }
    }

    public Optional<FXRateLock> useRateLock(UUID rateLockId) {
        logger.info("Using FX rate lock: {}", rateLockId);
        
        Optional<FXRateLock> lockOpt = rateLockRepository.findById(rateLockId);
        if (lockOpt.isEmpty()) {
            logger.warn("FX rate lock not found: {}", rateLockId);
            return Optional.empty();
        }

        FXRateLock lock = lockOpt.get();
        if (!lock.isUsable()) {
            logger.warn("FX rate lock not usable: {} (status: {}, expired: {})",
                       rateLockId, lock.getStatus(), lock.isExpired());
            return Optional.empty();
        }

        try {
            FXRateLock usedLock = lock.use();
            FXRateLock savedLock = rateLockRepository.save(usedLock);
            
            locksUsed.increment();
            logger.info("FX rate lock used: {} for {}", rateLockId, lock.getCurrencyPair());
            
            return Optional.of(savedLock);
            
        } catch (Exception e) {
            logger.error("Failed to use FX rate lock {}: {}", rateLockId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public Optional<FXRateLock> getRateLock(UUID rateLockId) {
        return rateLockRepository.findById(rateLockId);
    }

    @Transactional(readOnly = true)
    public List<FXRateLock> getActiveLocksForAccount(UUID accountId) {
        return rateLockRepository.findActiveByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<FXRateLock> getLocksForCorrelation(String correlationId) {
        return rateLockRepository.findByCorrelationId(correlationId);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveLock(String baseCurrency, String quoteCurrency, UUID accountId) {
        return rateLockRepository.existsActiveLock(baseCurrency, quoteCurrency, accountId);
    }

    public void expireRateLock(UUID rateLockId) {
        logger.info("Manually expiring FX rate lock: {}", rateLockId);
        
        Optional<FXRateLock> lockOpt = rateLockRepository.findById(rateLockId);
        if (lockOpt.isEmpty()) {
            logger.warn("FX rate lock not found: {}", rateLockId);
            return;
        }

        FXRateLock lock = lockOpt.get();
        if (lock.getStatus() != FXRateLock.FXRateLockStatus.ACTIVE) {
            logger.info("FX rate lock already in final state: {} (status: {})", rateLockId, lock.getStatus());
            return;
        }

        try {
            FXRateLock expiredLock = lock.expire();
            rateLockRepository.save(expiredLock);
            
            locksExpired.increment();
            logger.info("FX rate lock expired: {}", rateLockId);
            
        } catch (Exception e) {
            logger.error("Failed to expire FX rate lock {}: {}", rateLockId, e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void expireStaleRateLocks() {
        try {
            List<FXRateLock> expiredLocks = rateLockRepository.findExpiredActiveLocks();
            
            for (FXRateLock lock : expiredLocks) {
                try {
                    FXRateLock expiredLock = lock.expire();
                    rateLockRepository.save(expiredLock);
                    locksExpired.increment();
                } catch (Exception e) {
                    logger.error("Failed to expire stale rate lock {}: {}", lock.getId(), e.getMessage());
                }
            }
            
            if (!expiredLocks.isEmpty()) {
                logger.info("Expired {} stale FX rate locks", expiredLocks.size());
            }
            
        } catch (Exception e) {
            logger.error("Error during stale rate lock expiry: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupOldRateLocks() {
        try {
            Instant cutoff = Instant.now().minusSeconds(24 * 3600); // 24 hours ago
            int deleted = rateLockRepository.deleteOldLocks(cutoff);
            if (deleted > 0) {
                logger.info("Cleaned up {} old FX rate locks", deleted);
            }
        } catch (Exception e) {
            logger.error("Error during old rate lock cleanup: {}", e.getMessage(), e);
        }
    }

    public int getDefaultLockDurationMinutes() {
        return defaultLockDurationMinutes;
    }

    public int getMaxLockDurationMinutes() {
        return maxLockDurationMinutes;
    }
}