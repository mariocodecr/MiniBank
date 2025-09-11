package com.minibank.payments.adapter.persistence;

import com.minibank.payments.domain.IdempotencyKey;
import com.minibank.payments.domain.IdempotencyRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Repository
public class IdempotencyRedisRepository implements IdempotencyRepository {
    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;

    public IdempotencyRedisRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(IdempotencyKey idempotencyKey) {
        String key = buildKey(idempotencyKey.getRequestId());
        String value = idempotencyKey.getPaymentId().toString();
        
        redisTemplate.opsForValue().set(key, value, TTL.toSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public Optional<UUID> findPaymentIdByRequestId(String requestId) {
        String key = buildKey(requestId);
        String paymentId = redisTemplate.opsForValue().get(key);
        
        return paymentId != null ? 
            Optional.of(UUID.fromString(paymentId)) : 
            Optional.empty();
    }

    @Override
    public boolean exists(String requestId) {
        String key = buildKey(requestId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String buildKey(String requestId) {
        return KEY_PREFIX + requestId;
    }
}