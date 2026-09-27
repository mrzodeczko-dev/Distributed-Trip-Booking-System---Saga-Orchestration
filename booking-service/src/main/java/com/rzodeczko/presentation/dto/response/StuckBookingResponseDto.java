package com.rzodeczko.presentation.dto.response;

import com.rzodeczko.application.dto.SagaInstanceDto;
import com.rzodeczko.application.dto.StuckSagaDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Booking that stopped moving, with the reply it waits for, e.g. {@code waitingFor = "RESERVE HOTEL"}:
 * hotel-service has to answer the RESERVE command. Where that message got stuck (outbox, DLQ, ...) is not
 * known here - check the DLQs and the participant's outbox.
 */
public record StuckBookingResponseDto(
        String sagaId,
        String customerName,
        String destination,
        BigDecimal amount,
        String status,
        String waitingFor,
        List<SagaStepResponseDto> steps,
        Instant createdAt,
        Instant updatedAt
) {
    public static StuckBookingResponseDto from(StuckSagaDto dto) {
        SagaInstanceDto saga = dto.saga();
        return new StuckBookingResponseDto(
                saga.sagaId(),
                saga.customerName(),
                saga.destination(),
                saga.amount(),
                saga.status(),
                dto.waitingFor(),
                saga.steps().stream().map(SagaStepResponseDto::from).toList(),
                saga.createdAt(),
                saga.updatedAt()
        );
    }
}
