package com.rzodeczko.application.event;


import com.rzodeczko.domain.model.saga.SagaStepName;

import java.util.UUID;

public record SagaReply(
        UUID sagaId,
        SagaStepName step,
        SagaAction action,
        ReplyStatus status,
        String reason) {
}
