package com.rzodeczko.application.dto;

import com.rzodeczko.domain.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentDto(
        String id,
        String sagaId,
        String customerName,
        BigDecimal amount,
        String status,
        Instant createdAt
) {
    public static PaymentDto from(Payment payment) {
        return new PaymentDto(
                payment.getId().toString(),
                payment.getSagaId().toString(),
                payment.getCustomerName(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getCreatedAt()
        );
    }
}
