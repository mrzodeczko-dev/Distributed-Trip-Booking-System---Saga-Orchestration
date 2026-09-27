package com.rzodeczko.application.dto;

import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStepName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StuckSagaDtoTest {

    private static SagaInstance start() {
        return SagaInstance.start("Jan", "Mars", new BigDecimal("100.00"));
    }

    @Test
    void forwardPhaseWaitsForReserveReply() {
        SagaInstance saga = start();
        saga.markReserved(SagaStepName.FLIGHT);

        StuckSagaDto dto = StuckSagaDto.from(saga);

        assertThat(dto.saga().sagaId()).isEqualTo(saga.getId().toString());
        assertThat(dto.awaitedAction()).isEqualTo("RESERVE");
        assertThat(dto.awaitedStep()).isEqualTo("HOTEL");
        assertThat(dto.waitingFor()).isEqualTo("RESERVE HOTEL");
    }

    @Test
    void compensationWaitsForCancelReply() {
        SagaInstance saga = start();
        saga.markReserved(SagaStepName.FLIGHT);
        saga.failAndStartCompensation(SagaStepName.HOTEL, "No rooms");

        StuckSagaDto dto = StuckSagaDto.from(saga);

        assertThat(dto.awaitedAction()).isEqualTo("CANCEL");
        assertThat(dto.awaitedStep()).isEqualTo("FLIGHT");
        assertThat(dto.waitingFor()).isEqualTo("CANCEL FLIGHT");
    }

    @Test
    void terminalSagaWaitsForNothing() {
        SagaInstance saga = start();
        saga.failAndStartCompensation(SagaStepName.FLIGHT, "No seats");
        saga.cancel();

        StuckSagaDto dto = StuckSagaDto.from(saga);

        assertThat(dto.awaitedAction()).isNull();
        assertThat(dto.awaitedStep()).isNull();
        assertThat(dto.waitingFor()).isNull();
    }
}
