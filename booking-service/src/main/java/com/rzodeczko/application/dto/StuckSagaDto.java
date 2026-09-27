package com.rzodeczko.application.dto;

import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStepName;

import java.util.Optional;

/**
 * An active saga that has not changed for a while, together with the reply it is waiting for.
 *
 * @param awaitedAction RESERVE in the forward phase, CANCEL during compensation; {@code null} if nothing is awaited
 * @param awaitedStep   step whose participant has to reply; {@code null} if nothing is awaited
 */
public record StuckSagaDto(SagaInstanceDto saga, String awaitedAction, String awaitedStep) {

    public static StuckSagaDto from(SagaInstance saga) {
        Optional<SagaStepName> step = saga.awaitedStep();
        String action = step.isEmpty()
                ? null
                : (saga.isForwardPhase() ? SagaAction.RESERVE : SagaAction.CANCEL).name();
        return new StuckSagaDto(SagaInstanceDto.from(saga), action, step.map(Enum::name).orElse(null));
    }

    /**
     * E.g. {@code "RESERVE HOTEL"}, or {@code null} if the saga waits for nothing.
     */
    public String waitingFor() {
        return awaitedStep == null ? null : awaitedAction + " " + awaitedStep;
    }
}
