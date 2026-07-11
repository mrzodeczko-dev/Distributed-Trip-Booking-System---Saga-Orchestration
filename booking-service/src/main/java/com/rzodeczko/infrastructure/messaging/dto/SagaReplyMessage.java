package com.rzodeczko.infrastructure.messaging.dto;

import java.util.UUID;

public record SagaReplyMessage(
        UUID sagaId,
        String step,
        String action,
        String status,
        String reason) {
}
