package com.minibank.fx.adapter.persistence;

import com.minibank.fx.domain.ExchangeRate;
import com.minibank.fx.domain.ExchangeRateRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class ExchangeRateRepositoryImpl implements ExchangeRateRepository {
    
    private final ExchangeRateJpaRepository jpaRepository;

    public ExchangeRateRepositoryImpl(ExchangeRateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ExchangeRate save(ExchangeRate exchangeRate) {
        ExchangeRateEntity entity = toEntity(exchangeRate);
        ExchangeRateEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ExchangeRate> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ExchangeRate> findLatestRate(String baseCurrency, String quoteCurrency) {
        return jpaRepository.findLatestRate(baseCurrency, quoteCurrency).map(this::toDomain);
    }

    @Override
    public List<ExchangeRate> findLatestRatesByProvider(String provider) {
        return jpaRepository.findLatestRatesByProvider(provider).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<ExchangeRate> findRatesAfter(Instant timestamp) {
        return jpaRepository.findRatesAfter(timestamp).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<ExchangeRate> findExpiredRates() {
        return jpaRepository.findExpiredRates(Instant.now()).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void deleteExpiredRates(Instant before) {
        jpaRepository.deleteExpiredRatesBefore(before);
    }

    @Override
    public boolean existsValidRate(String baseCurrency, String quoteCurrency) {
        return jpaRepository.existsValidRate(baseCurrency, quoteCurrency, Instant.now());
    }

    private ExchangeRateEntity toEntity(ExchangeRate rate) {
        return new ExchangeRateEntity(
            rate.getId(),
            rate.getBaseCurrency(),
            rate.getQuoteCurrency(),
            rate.getRate(),
            rate.getSpread(),
            rate.getProvider(),
            rate.getTimestamp(),
            rate.getValidUntil()
        );
    }

    private ExchangeRate toDomain(ExchangeRateEntity entity) {
        return ExchangeRate.fromEntity(
            entity.getId(),
            entity.getBaseCurrency(),
            entity.getQuoteCurrency(),
            entity.getRate(),
            entity.getSpread(),
            entity.getProvider(),
            entity.getTimestamp(),
            entity.getValidUntil()
        );
    }
}