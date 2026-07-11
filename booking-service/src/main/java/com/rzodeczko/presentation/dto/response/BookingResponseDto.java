package com.rzodeczko.presentation.dto.response;

import com.rzodeczko.application.dto.SagaInstanceDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingResponseDto(
        String sagaId,
        String customerName,
        String destination,
        BigDecimal amount,
        String status,
        List<SagaStepResponseDto> steps,
        Instant createdAt,
        Instant updatedAt
) {
    public static BookingResponseDto from(SagaInstanceDto dto) {
        return new BookingResponseDto(
                dto.sagaId(),
                dto.customerName(),
                dto.destination(),
                dto.amount(),
                dto.status(),
                dto.steps().stream().map(SagaStepResponseDto::from).toList(),
                dto.createdAt(),
                dto.updatedAt()
        );
    }
}
