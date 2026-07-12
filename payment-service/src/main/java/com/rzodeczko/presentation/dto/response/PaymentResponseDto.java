package com.rzodeczko.presentation.dto.response;

import com.rzodeczko.application.dto.PaymentDto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponseDto(
        String id,
        String sagaId,
        String customerName,
        BigDecimal amount,
        String status,
        Instant createdAt
) {
    public static PaymentResponseDto from(PaymentDto dto) {
        return new PaymentResponseDto(
                dto.id(),
                dto.sagaId(),
                dto.customerName(),
                dto.amount(),
                dto.status(),
                dto.createdAt()
        );
    }
}
