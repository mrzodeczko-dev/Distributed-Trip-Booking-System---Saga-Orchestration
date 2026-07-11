package com.rzodeczko.infrastructure.messaging.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kontrakt komendy odbieranej z kolejki q.flight-service.commands.
 * Ksztalt zgodny z ParticipantCommandMessage wysylanym przez orkiestrator.
 */
public record FlightCommandMessageDto(
        UUID sagaId,
        String action,
        String customerName,
        String destination,
        BigDecimal amount
) {
}
