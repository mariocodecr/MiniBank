package com.minibank.accounts.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountCurrencyBalanceJpaRepository extends JpaRepository<AccountCurrencyBalanceEntity, UUID> {
    
    List<AccountCurrencyBalanceEntity> findByAccountIdOrderByCurrency(UUID accountId);
    
    Optional<AccountCurrencyBalanceEntity> findByAccountIdAndCurrency(UUID accountId, String currency);
    
    @Query("SELECT b FROM AccountCurrencyBalanceEntity b WHERE b.accountId = :accountId AND b.currency = :currency FOR UPDATE")
    Optional<AccountCurrencyBalanceEntity> findByAccountIdAndCurrencyForUpdate(@Param("accountId") UUID accountId, @Param("currency") String currency);
    
    @Query("SELECT COALESCE(SUM(b.totalAmountMinor), 0) FROM AccountCurrencyBalanceEntity b WHERE b.currency = :currency")
    Long getTotalBalanceByCurrency(@Param("currency") String currency);
    
    @Query("SELECT b.currency, SUM(b.totalAmountMinor) as totalBalance FROM AccountCurrencyBalanceEntity b GROUP BY b.currency")
    List<Object[]> getTotalBalancesByAllCurrencies();
    
    @Query("SELECT COUNT(DISTINCT b.accountId) FROM AccountCurrencyBalanceEntity b WHERE b.currency = :currency AND b.totalAmountMinor > 0")
    Long countAccountsWithCurrencyBalance(@Param("currency") String currency);
    
    void deleteByAccountId(UUID accountId);
}