package com.minibank.fx.adapter.persistence;

import com.minibank.fx.domain.FXConversion;
import com.minibank.fx.domain.FXConversionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class FXConversionRepositoryImpl implements FXConversionRepository {
    
    private final FXConversionJpaRepository jpaRepository;

    public FXConversionRepositoryImpl(FXConversionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public FXConversion save(FXConversion conversion) {
        FXConversionEntity entity = toEntity(conversion);
        FXConversionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<FXConversion> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<FXConversion> findByAccountId(UUID accountId) {
        return jpaRepository.findByAccountIdOrderByTimestampDesc(accountId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<FXConversion> findByAccountIdAndCurrencyPair(UUID accountId, String fromCurrency, String toCurrency) {
        return jpaRepository.findByAccountIdAndCurrencyPair(accountId, fromCurrency, toCurrency).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<FXConversion> findByCorrelationId(String correlationId) {
        return jpaRepository.findByCorrelationIdOrderByTimestampDesc(correlationId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<FXConversion> findConversionsAfter(Instant timestamp) {
        return jpaRepository.findConversionsAfter(timestamp).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<FXConversion> findConversionsBetween(Instant startTime, Instant endTime) {
        return jpaRepository.findConversionsBetween(startTime, endTime).stream()
            .map(this::toDomain)
            .toList();
    }

    private FXConversionEntity toEntity(FXConversion conversion) {
        return new FXConversionEntity(
            conversion.getId(),
            conversion.getAccountId(),
            conversion.getFromCurrency(),
            conversion.getToCurrency(),
            conversion.getFromAmountMinor(),
            conversion.getToAmountMinor(),
            conversion.getExchangeRate(),
            conversion.getSpread(),
            conversion.getProvider(),
            conversion.getTimestamp(),
            conversion.getCorrelationId()
        );
    }

    private FXConversion toDomain(FXConversionEntity entity) {
        return FXConversion.fromEntity(
            entity.getId(),
            entity.getAccountId(),
            entity.getFromCurrency(),
            entity.getToCurrency(),
            entity.getFromAmountMinor(),
            entity.getToAmountMinor(),
            entity.getExchangeRate(),
            entity.getSpread(),
            entity.getProvider(),
            entity.getTimestamp(),
            entity.getCorrelationId()
        );
    }
}