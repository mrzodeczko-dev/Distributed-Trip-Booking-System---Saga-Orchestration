package com.rzodeczko.application.command;

import com.rzodeczko.application.event.SagaAction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Aplikacyjna komenda dla flight-service (mapowana z wiadomosci AMQP).
 */
public record FlightCommand(
        UUID sagaId,
        SagaAction action,
        String customerName,
        String destination,
        BigDecimal amount
) {
}
