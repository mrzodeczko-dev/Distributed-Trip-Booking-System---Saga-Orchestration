package com.rzodeczko.infrastructure.messaging.dto;

import java.util.UUID;

/**
 * Kontrakt odpowiedzi publikowanej na x.saga.replies (odbiera ja orkiestrator).
 */
public record SagaReplyMessageDto(
        UUID sagaId,
        String step,
        String action,
        String status,
        String reason
) {
}
