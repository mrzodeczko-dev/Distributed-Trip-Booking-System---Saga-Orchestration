package com.rzodeczko.application.dto;


import com.rzodeczko.domain.model.saga.SagaInstance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SagaInstanceDto(
        String sagaId,
        String customerName,
        String destination,
        BigDecimal amount,
        String status,
        List<SagaStepDto> steps,
        Instant createdAt,
        Instant updatedAt
) {
    public static SagaInstanceDto from(SagaInstance saga) {
        return new SagaInstanceDto(
                saga.getId().toString(),
                saga.getCustomerName(),
                saga.getDestination(),
                saga.getAmount(),
                saga.getStatus().name(),
                saga.getSteps().stream().map(SagaStepDto::from).toList(),
                saga.getCreatedAt(),
                saga.getUpdatedAt()
        );
    }
}
