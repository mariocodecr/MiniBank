package com.minibank.fx.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.fx.domain.ExchangeRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RedisFXRateCache implements FXRateCache {
    private static final Logger logger = LoggerFactory.getLogger(RedisFXRateCache.class);
    private static final String KEY_PREFIX = "fx:rate:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisFXRateCache(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void put(String baseCurrency, String quoteCurrency, ExchangeRate rate) {
        put(baseCurrency, quoteCurrency, rate, DEFAULT_TTL);
    }

    @Override
    public void put(String baseCurrency, String quoteCurrency, ExchangeRate rate, Duration ttl) {
        try {
            String key = buildKey(baseCurrency, quoteCurrency);
            String value = objectMapper.writeValueAsString(toDto(rate));
            
            redisTemplate.opsForValue().set(key, value, ttl);
            logger.debug("Cached exchange rate for {} with TTL {}", key, ttl);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize exchange rate for caching: {}", e.getMessage());
        }
    }

    @Override
    public Optional<ExchangeRate> get(String baseCurrency, String quoteCurrency) {
        try {
            String key = buildKey(baseCurrency, quoteCurrency);
            String value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                logger.debug("Cache miss for {}", key);
                return Optional.empty();
            }
            
            ExchangeRateDto dto = objectMapper.readValue(value, ExchangeRateDto.class);
            ExchangeRate rate = fromDto(dto);
            
            if (rate.isExpired()) {
                logger.debug("Cached rate expired for {}, removing", key);
                remove(baseCurrency, quoteCurrency);
                return Optional.empty();
            }
            
            logger.debug("Cache hit for {}", key);
            return Optional.of(rate);
            
        } catch (Exception e) {
            logger.error("Failed to deserialize cached exchange rate: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void remove(String baseCurrency, String quoteCurrency) {
        String key = buildKey(baseCurrency, quoteCurrency);
        redisTemplate.delete(key);
        logger.debug("Removed cached rate for {}", key);
    }

    @Override
    public void removeByProvider(String provider) {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                Optional<ExchangeRate> rate = get(extractBaseCurrency(key), extractQuoteCurrency(key));
                if (rate.isPresent() && provider.equals(rate.get().getProvider())) {
                    redisTemplate.delete(key);
                    logger.debug("Removed cached rate from provider {} for {}", provider, key);
                }
            }
        }
    }

    @Override
    public void clear() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            logger.info("Cleared {} cached exchange rates", keys.size());
        }
    }

    @Override
    public List<String> getAllCachedPairs() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) {
            return List.of();
        }
        
        return keys.stream()
            .map(key -> key.substring(KEY_PREFIX.length()))
            .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String baseCurrency, String quoteCurrency) {
        String key = buildKey(baseCurrency, quoteCurrency);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void evictExpired() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null) {
            int evicted = 0;
            for (String key : keys) {
                String baseCurrency = extractBaseCurrency(key);
                String quoteCurrency = extractQuoteCurrency(key);
                Optional<ExchangeRate> rate = get(baseCurrency, quoteCurrency);
                if (rate.isEmpty()) {
                    evicted++;
                }
            }
            if (evicted > 0) {
                logger.info("Evicted {} expired exchange rates from cache", evicted);
            }
        }
    }

    private String buildKey(String baseCurrency, String quoteCurrency) {
        return KEY_PREFIX + baseCurrency + ":" + quoteCurrency;
    }

    private String extractBaseCurrency(String key) {
        String pair = key.substring(KEY_PREFIX.length());
        return pair.split(":")[0];
    }

    private String extractQuoteCurrency(String key) {
        String pair = key.substring(KEY_PREFIX.length());
        return pair.split(":")[1];
    }

    private ExchangeRateDto toDto(ExchangeRate rate) {
        return new ExchangeRateDto(
            rate.getId().toString(),
            rate.getBaseCurrency(),
            rate.getQuoteCurrency(),
            rate.getRate(),
            rate.getSpread(),
            rate.getProvider(),
            rate.getTimestamp().toEpochMilli(),
            rate.getValidUntil().toEpochMilli()
        );
    }

    private ExchangeRate fromDto(ExchangeRateDto dto) {
        return ExchangeRate.fromEntity(
            UUID.fromString(dto.id),
            dto.baseCurrency,
            dto.quoteCurrency,
            dto.rate,
            dto.spread,
            dto.provider,
            Instant.ofEpochMilli(dto.timestamp),
            Instant.ofEpochMilli(dto.validUntil)
        );
    }

    private static class ExchangeRateDto {
        public String id;
        public String baseCurrency;
        public String quoteCurrency;
        public BigDecimal rate;
        public BigDecimal spread;
        public String provider;
        public long timestamp;
        public long validUntil;

        public ExchangeRateDto() {}

        public ExchangeRateDto(String id, String baseCurrency, String quoteCurrency,
                              BigDecimal rate, BigDecimal spread, String provider,
                              long timestamp, long validUntil) {
            this.id = id;
            this.baseCurrency = baseCurrency;
            this.quoteCurrency = quoteCurrency;
            this.rate = rate;
            this.spread = spread;
            this.provider = provider;
            this.timestamp = timestamp;
            this.validUntil = validUntil;
        }
    }
}