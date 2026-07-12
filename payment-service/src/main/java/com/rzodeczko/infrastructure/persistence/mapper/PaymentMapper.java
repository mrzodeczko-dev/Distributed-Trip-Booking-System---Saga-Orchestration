package com.rzodeczko.infrastructure.persistence.mapper;

import com.rzodeczko.domain.model.Payment;
import com.rzodeczko.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentEntity toEntity(Payment payment) {
        return PaymentEntity.builder()
                .id(payment.getId())
                .sagaId(payment.getSagaId())
                .customerName(payment.getCustomerName())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public Payment toDomain(PaymentEntity entity) {
        return Payment.restore(
                entity.getId(),
                entity.getSagaId(),
                entity.getCustomerName(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
