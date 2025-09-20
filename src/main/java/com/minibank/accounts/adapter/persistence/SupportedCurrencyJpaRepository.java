package com.minibank.accounts.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportedCurrencyJpaRepository extends JpaRepository<SupportedCurrencyEntity, String> {
    
    List<SupportedCurrencyEntity> findByIsActiveTrueOrderByCurrencyCode();
    
    Optional<SupportedCurrencyEntity> findByCurrencyCodeAndIsActiveTrue(String currencyCode);
    
    @Query("SELECT c.currencyCode FROM SupportedCurrencyEntity c WHERE c.isActive = true ORDER BY c.currencyCode")
    List<String> findActiveCurrencyCodes();
    
    boolean existsByCurrencyCodeAndIsActiveTrue(String currencyCode);
}