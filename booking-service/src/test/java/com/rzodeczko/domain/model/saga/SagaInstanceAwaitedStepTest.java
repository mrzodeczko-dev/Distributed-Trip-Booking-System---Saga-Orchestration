package com.rzodeczko.domain.model.saga;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SagaInstanceAwaitedStepTest {

    private static SagaInstance start() {
        return SagaInstance.start("Jan", "Mars", new BigDecimal("100.00"));
    }

    @Test
    void newSagaWaitsForFirstStep() {
        assertThat(start().awaitedStep()).contains(SagaStepName.FLIGHT);
    }

    @Test
    void forwardPhaseWaitsForNextPendingStep() {
        SagaInstance saga = start();
        saga.markReserved(SagaStepName.FLIGHT);

        assertThat(saga.awaitedStep()).contains(SagaStepName.HOTEL);
    }

    @Test
    void compensationWaitsForLastReservedStep() {
        SagaInstance saga = start();
        saga.markReserved(SagaStepName.FLIGHT);
        saga.markReserved(SagaStepName.HOTEL);
        saga.failAndStartCompensation(SagaStepName.PAYMENT, "Card declined");

        assertThat(saga.awaitedStep()).contains(SagaStepName.HOTEL);

        saga.markCompensated(SagaStepName.HOTEL);

        assertThat(saga.awaitedStep()).contains(SagaStepName.FLIGHT);
    }

    @Test
    void terminalSagaWaitsForNothing() {
        SagaInstance completed = start();
        completed.markReserved(SagaStepName.FLIGHT);
        completed.markReserved(SagaStepName.HOTEL);
        completed.markReserved(SagaStepName.PAYMENT);
        completed.complete();

        SagaInstance cancelled = start();
        cancelled.failAndStartCompensation(SagaStepName.FLIGHT, "No seats");
        cancelled.cancel();

        assertThat(completed.awaitedStep()).isEmpty();
        assertThat(cancelled.awaitedStep()).isEmpty();
    }
}
