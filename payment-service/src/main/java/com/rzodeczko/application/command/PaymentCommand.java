package com.rzodeczko.application.command;

import com.rzodeczko.application.event.SagaAction;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCommand(
        UUID sagaId,
        SagaAction action,
        String customerName,
        String destination,
        BigDecimal amount
) {
}
