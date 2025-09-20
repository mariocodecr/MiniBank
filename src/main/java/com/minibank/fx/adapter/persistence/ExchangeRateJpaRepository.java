package com.minibank.fx.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExchangeRateJpaRepository extends JpaRepository<ExchangeRateEntity, UUID> {
    
    @Query("SELECT e FROM ExchangeRateEntity e WHERE e.baseCurrency = :baseCurrency " +
           "AND e.quoteCurrency = :quoteCurrency AND e.validUntil > :now " +
           "ORDER BY e.timestamp DESC LIMIT 1")
    Optional<ExchangeRateEntity> findLatestValidRate(@Param("baseCurrency") String baseCurrency,
                                                     @Param("quoteCurrency") String quoteCurrency,
                                                     @Param("now") Instant now);

    @Query("SELECT e FROM ExchangeRateEntity e WHERE e.baseCurrency = :baseCurrency " +
           "AND e.quoteCurrency = :quoteCurrency ORDER BY e.timestamp DESC LIMIT 1")
    Optional<ExchangeRateEntity> findLatestRate(@Param("baseCurrency") String baseCurrency,
                                               @Param("quoteCurrency") String quoteCurrency);

    @Query("SELECT e FROM ExchangeRateEntity e WHERE e.provider = :provider " +
           "AND e.validUntil > :now ORDER BY e.timestamp DESC")
    List<ExchangeRateEntity> findLatestValidRatesByProvider(@Param("provider") String provider,
                                                           @Param("now") Instant now);

    @Query("SELECT e FROM ExchangeRateEntity e WHERE e.provider = :provider " +
           "ORDER BY e.timestamp DESC")
    List<ExchangeRateEntity> findLatestRatesByProvider(@Param("provider") String provider);

    @Query("SELECT e FROM ExchangeRateEntity e WHERE e.timestamp > :timestamp " +
           "ORDER BY e.timestamp DESC")
    List<ExchangeRateEntity> findRatesAfter(@Param("timestamp") Instant timestamp);

    @Query("SELECT e FROM ExchangeRateEntity e WHERE e.validUntil < :now")
    List<ExchangeRateEntity> findExpiredRates(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM ExchangeRateEntity e WHERE e.validUntil < :cutoff")
    void deleteExpiredRatesBefore(@Param("cutoff") Instant cutoff);

    @Query("SELECT COUNT(e) > 0 FROM ExchangeRateEntity e WHERE e.baseCurrency = :baseCurrency " +
           "AND e.quoteCurrency = :quoteCurrency AND e.validUntil > :now")
    boolean existsValidRate(@Param("baseCurrency") String baseCurrency,
                           @Param("quoteCurrency") String quoteCurrency,
                           @Param("now") Instant now);

    @Query("SELECT DISTINCT e.provider FROM ExchangeRateEntity e")
    List<String> findAllProviders();

    @Query("SELECT COUNT(e) FROM ExchangeRateEntity e WHERE e.provider = :provider " +
           "AND e.validUntil > :now")
    long countValidRatesByProvider(@Param("provider") String provider, @Param("now") Instant now);
}