package com.rzodeczko.application.event;

import java.util.UUID;

/**
 * Aplikacyjna odpowiedz uczestnika Sagi wysylana do orkiestratora.
 */
public record SagaParticipantReply(
        UUID sagaId,
        String step,
        SagaAction action,
        String status,
        String reason
) {
    public static SagaParticipantReply from(
            UUID sagaId,
            String step,
            SagaAction action,
            CommandResult result) {
        return new SagaParticipantReply(sagaId, step, action, result.statusString(), result.reason());
    }
}
