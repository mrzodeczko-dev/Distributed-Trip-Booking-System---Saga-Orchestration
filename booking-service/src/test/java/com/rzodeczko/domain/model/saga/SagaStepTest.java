package com.rzodeczko.domain.model.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SagaStepTest {

    @Test
    @DisplayName("New step should be PENDING")
    void newStepShouldBePending() {
        SagaStep step = new SagaStep(SagaStepName.FLIGHT);

        assertThat(step.getStatus()).isEqualTo(SagaStepStatus.PENDING);
        assertThat(step.getName()).isEqualTo(SagaStepName.FLIGHT);
        assertThat(step.getReason()).isNull();
        assertThat(step.isPending()).isTrue();
        assertThat(step.isReserved()).isFalse();
    }

    @Test
    @DisplayName("restore() should recreate step with given state")
    void restoreShouldRecreateState() {
        SagaStep step = SagaStep.restore(SagaStepName.HOTEL, SagaStepStatus.FAILED, "No rooms");

        assertThat(step.getName()).isEqualTo(SagaStepName.HOTEL);
        assertThat(step.getStatus()).isEqualTo(SagaStepStatus.FAILED);
        assertThat(step.getReason()).isEqualTo("No rooms");
        assertThat(step.isPending()).isFalse();
        assertThat(step.isReserved()).isFalse();
    }

    @Test
    @DisplayName("isReserved() should return true only for RESERVED status")
    void isReservedShouldWork() {
        SagaStep step = SagaStep.restore(SagaStepName.PAYMENT, SagaStepStatus.RESERVED, null);

        assertThat(step.isReserved()).isTrue();
        assertThat(step.isPending()).isFalse();
    }
}
