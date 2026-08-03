package com.rzodeczko.infrastructure.persistence.adapter;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.port.out.PaymentRepository;
import com.rzodeczko.domain.model.Payment;
import com.rzodeczko.infrastructure.persistence.entity.PaymentEntity;
import com.rzodeczko.infrastructure.persistence.mapper.PaymentMapper;
import com.rzodeczko.infrastructure.persistence.repository.JpaPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

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
    public PageResult<Payment> findAll(PageQuery query) {
        Page<PaymentEntity> page = jpaPaymentRepository.findAll(
                PageRequest.of(query.page(), query.size(), Sort.by("createdAt").descending()));
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
