package com.minibank.fx.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FXConversionJpaRepository extends JpaRepository<FXConversionEntity, UUID> {
    
    List<FXConversionEntity> findByAccountIdOrderByTimestampDesc(UUID accountId);

    @Query("SELECT c FROM FXConversionEntity c WHERE c.accountId = :accountId " +
           "AND c.fromCurrency = :fromCurrency AND c.toCurrency = :toCurrency " +
           "ORDER BY c.timestamp DESC")
    List<FXConversionEntity> findByAccountIdAndCurrencyPair(@Param("accountId") UUID accountId,
                                                           @Param("fromCurrency") String fromCurrency,
                                                           @Param("toCurrency") String toCurrency);

    List<FXConversionEntity> findByCorrelationIdOrderByTimestampDesc(String correlationId);

    @Query("SELECT c FROM FXConversionEntity c WHERE c.timestamp > :timestamp " +
           "ORDER BY c.timestamp DESC")
    List<FXConversionEntity> findConversionsAfter(@Param("timestamp") Instant timestamp);

    @Query("SELECT c FROM FXConversionEntity c WHERE c.timestamp >= :startTime " +
           "AND c.timestamp <= :endTime ORDER BY c.timestamp DESC")
    List<FXConversionEntity> findConversionsBetween(@Param("startTime") Instant startTime,
                                                   @Param("endTime") Instant endTime);

    @Query("SELECT c FROM FXConversionEntity c WHERE c.provider = :provider " +
           "ORDER BY c.timestamp DESC")
    List<FXConversionEntity> findByProvider(@Param("provider") String provider);

    @Query("SELECT DISTINCT c.fromCurrency FROM FXConversionEntity c")
    List<String> findDistinctFromCurrencies();

    @Query("SELECT DISTINCT c.toCurrency FROM FXConversionEntity c")
    List<String> findDistinctToCurrencies();

    @Query("SELECT COUNT(c) FROM FXConversionEntity c WHERE c.accountId = :accountId")
    long countByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT SUM(c.fromAmountMinor) FROM FXConversionEntity c WHERE c.accountId = :accountId " +
           "AND c.fromCurrency = :currency")
    Long sumFromAmountByAccountIdAndCurrency(@Param("accountId") UUID accountId,
                                           @Param("currency") String currency);

    @Query("SELECT SUM(c.toAmountMinor) FROM FXConversionEntity c WHERE c.accountId = :accountId " +
           "AND c.toCurrency = :currency")
    Long sumToAmountByAccountIdAndCurrency(@Param("accountId") UUID accountId,
                                         @Param("currency") String currency);
}