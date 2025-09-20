package com.minibank.accounts.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.minibank.accounts.domain.Currency;

@Repository
public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {
    
    List<AccountEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    Optional<AccountEntity> findByAccountNumber(String accountNumber);
    Optional<AccountEntity> findByEmail(String email);
    boolean existsByAccountNumber(String accountNumber);
    boolean existsByEmail(String email);
    
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AccountEntity a " +
           "WHERE a.userId = :userId AND a.currency = :currency")
    boolean existsByUserIdAndCurrency(@Param("userId") UUID userId, @Param("currency") Currency currency);
}