package com.minibank.payments.adapter.persistence;

import com.minibank.payments.domain.Payment;
import com.minibank.payments.domain.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    
    private final PaymentJpaRepository jpaRepository;
    private final PaymentEntityMapper mapper;
    
    public PaymentRepositoryImpl(PaymentJpaRepository jpaRepository, PaymentEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = mapper.toEntity(payment);
        PaymentEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return jpaRepository.findById(paymentId)
                .map(mapper::toDomain);
    }
    
    @Override
    public Optional<Payment> findByRequestId(String requestId) {
        return jpaRepository.findByRequestId(requestId)
                .map(mapper::toDomain);
    }
    
    @Override
    public boolean existsByRequestId(String requestId) {
        return jpaRepository.existsByRequestId(requestId);
    }
}