package com.rzodeczko.infrastructure.persistence.adapter;

import com.rzodeczko.application.port.out.PaymentRepository;
import com.rzodeczko.domain.model.Payment;
import com.rzodeczko.infrastructure.persistence.mapper.PaymentMapper;
import com.rzodeczko.infrastructure.persistence.repository.JpaPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final JpaPaymentRepository jpaPaymentRepository;
    private final PaymentMapper mapper;

    @Override
    public void save(Payment payment) {
        jpaPaymentRepository.save(
                jpaPaymentRepository.findById(payment.getId())
                        .map(existing -> {
                            existing.setStatus(payment.getStatus());
                            return existing;
                        })
                        .orElseGet(() -> mapper.toEntity(payment))
        );
    }

    @Override
    public boolean existsBySagaId(UUID sagaId) {
        return jpaPaymentRepository.existsBySagaId(sagaId);
    }

    @Override
    public Optional<Payment> findBySagaId(UUID sagaId) {
        return jpaPaymentRepository.findBySagaId(sagaId).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findAll() {
        return jpaPaymentRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}