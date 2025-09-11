package com.minibank.ledger.adapter.persistence;

import com.minibank.ledger.domain.LedgerEntry;
import com.minibank.ledger.domain.LedgerEntryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class LedgerEntryRepositoryImpl implements LedgerEntryRepository {
    
    private final LedgerEntryJpaRepository jpaRepository;
    private final LedgerEntryEntityMapper mapper;
    
    public LedgerEntryRepositoryImpl(LedgerEntryJpaRepository jpaRepository, LedgerEntryEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public LedgerEntry save(LedgerEntry ledgerEntry) {
        LedgerEntryEntity entity = mapper.toEntity(ledgerEntry);
        LedgerEntryEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public List<LedgerEntry> findByPaymentId(UUID paymentId) {
        return jpaRepository.findByPaymentIdOrderByOccurredAtAsc(paymentId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
    
    @Override
    public List<LedgerEntry> saveAll(List<LedgerEntry> entries) {
        List<LedgerEntryEntity> entities = entries.stream()
                .map(mapper::toEntity)
                .toList();
        
        List<LedgerEntryEntity> savedEntities = jpaRepository.saveAll(entities);
        
        return savedEntities.stream()
                .map(mapper::toDomain)
                .toList();
    }
}