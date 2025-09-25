package com.minibank.fx.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FXProviderRepository extends JpaRepository<FXProviderEntity, String> {

    @Query("SELECT p FROM FXProviderEntity p WHERE p.isActive = true ORDER BY p.priorityOrder")
    List<FXProviderEntity> findActiveProvidersOrderedByPriority();

    Optional<FXProviderEntity> findByProviderCodeAndIsActive(String providerCode, Boolean isActive);

    @Query("SELECT p FROM FXProviderEntity p WHERE p.isActive = true AND p.priorityOrder = " +
           "(SELECT MIN(p2.priorityOrder) FROM FXProviderEntity p2 WHERE p2.isActive = true)")
    Optional<FXProviderEntity> findHighestPriorityActiveProvider();
}