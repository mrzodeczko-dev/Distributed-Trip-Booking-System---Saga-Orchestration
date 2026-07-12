package com.rzodeczko.infrastructure.messaging.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCommandMessage(
        UUID sagaId,
        String action,
        String customerName,
        String destination,
        BigDecimal amount
) {
}