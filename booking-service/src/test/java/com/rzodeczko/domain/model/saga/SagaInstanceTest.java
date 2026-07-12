package com.rzodeczko.domain.model.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class SagaInstanceTest {

    private static final String CUSTOMER = "Jan Kowalski";
    private static final String DESTINATION = "Mars";
    private static final BigDecimal AMOUNT = new BigDecimal("1500.00");

    private SagaInstance startSaga() {
        return SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);
    }

    // ------------------------------------------------------------------
    // start()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("start()")
    class Start {

        @Test
        void shouldCreateSagaWithInProgressStatus() {
            SagaInstance saga = startSaga();

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.IN_PROGRESS);
            assertThat(saga.getId()).isNotNull();
            assertThat(saga.getCustomerName()).isEqualTo(CUSTOMER);
            assertThat(saga.getDestination()).isEqualTo(DESTINATION);
            assertThat(saga.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(saga.getCreatedAt()).isNotNull();
            assertThat(saga.getUpdatedAt()).isNotNull();
        }

        @Test
        void shouldCreateAllStepsAsPending() {
            SagaInstance saga = startSaga();

            assertThat(saga.getSteps())
                    .hasSize(SagaStepName.values().length)
                    .allSatisfy(step ->
                            assertThat(step.getStatus()).isEqualTo(SagaStepStatus.PENDING)
                    );
        }

        @Test
        void shouldRejectNullCustomerName() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> SagaInstance.start(null, DESTINATION, AMOUNT))
                    .withMessageContaining("customerName");
        }

        @Test
        void shouldRejectBlankCustomerName() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> SagaInstance.start("  ", DESTINATION, AMOUNT))
                    .withMessageContaining("customerName");
        }

        @Test
        void shouldRejectNullDestination() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> SagaInstance.start(CUSTOMER, null, AMOUNT))
                    .withMessageContaining("destination");
        }

        @Test
        void shouldRejectNullAmount() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> SagaInstance.start(CUSTOMER, DESTINATION, null))
                    .withMessageContaining("amount");
        }

        @Test
        void shouldRejectZeroAmount() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> SagaInstance.start(CUSTOMER, DESTINATION, BigDecimal.ZERO))
                    .withMessageContaining("amount");
        }

        @Test
        void shouldRejectNegativeAmount() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> SagaInstance.start(CUSTOMER, DESTINATION, new BigDecimal("-100")))
                    .withMessageContaining("amount");
        }
    }

    // ------------------------------------------------------------------
    // nextStepToReserve()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("nextStepToReserve()")
    class NextStepToReserve {

        @Test
        void shouldReturnFirstStepForNewSaga() {
            SagaInstance saga = startSaga();

            assertThat(saga.nextStepToReserve())
                    .hasValue(SagaStepName.FLIGHT);
        }

        @Test
        void shouldReturnSecondStepAfterFirstReserved() {
            SagaInstance saga = startSaga();
            saga.markReserved(SagaStepName.FLIGHT);

            assertThat(saga.nextStepToReserve())
                    .hasValue(SagaStepName.HOTEL);
        }

        @Test
        void shouldReturnEmptyWhenAllReserved() {
            SagaInstance saga = startSaga();
            saga.markReserved(SagaStepName.FLIGHT);
            saga.markReserved(SagaStepName.HOTEL);
            saga.markReserved(SagaStepName.PAYMENT);

            assertThat(saga.nextStepToReserve()).isEmpty();
        }
    }

    // ------------------------------------------------------------------
    // nextStepToCompensate()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("nextStepToCompensate()")
    class NextStepToCompensate {

        @Test
        void shouldReturnEmptyWhenNoStepsReserved() {
            SagaInstance saga = startSaga();

            assertThat(saga.nextStepToCompensate()).isEmpty();
        }

        @Test
        void shouldReturnLastReservedStep() {
            SagaInstance saga = startSaga();
            saga.markReserved(SagaStepName.FLIGHT);
            saga.markReserved(SagaStepName.HOTEL);

            assertThat(saga.nextStepToCompensate())
                    .hasValue(SagaStepName.HOTEL);
        }

        @Test
        void shouldReturnFirstReservedWhenOnlyOneReserved() {
            SagaInstance saga = startSaga();
            saga.markReserved(SagaStepName.FLIGHT);

            assertThat(saga.nextStepToCompensate())
                    .hasValue(SagaStepName.FLIGHT);
        }
    }

    // ------------------------------------------------------------------
    // Forward flow: markReserved() → complete()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Forward flow")
    class ForwardFlow {

        @Test
        void shouldCompleteAfterAllStepsReserved() {
            SagaInstance saga = startSaga();
            saga.markReserved(SagaStepName.FLIGHT);
            saga.markReserved(SagaStepName.HOTEL);
            saga.markReserved(SagaStepName.PAYMENT);
            saga.complete();

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        }

        @Test
        void shouldMarkStepAsReserved() {
            SagaInstance saga = startSaga();
            saga.markReserved(SagaStepName.FLIGHT);

            assertThat(saga.getStep(SagaStepName.FLIGHT).getStatus())
                    .isEqualTo(SagaStepStatus.RESERVED);
        }

        @Test
        void shouldUpdateTimestampOnReserve() {
            SagaInstance saga = startSaga();
            var before = saga.getUpdatedAt();
            saga.markReserved(SagaStepName.FLIGHT);

            assertThat(saga.getUpdatedAt()).isAfterOrEqualTo(before);
        }

        @Test
        void shouldBeInForwardPhaseWhenInProgress() {
            SagaInstance saga = startSaga();

            assertThat(saga.isForwardPhase()).isTrue();
            assertThat(saga.isCompensating()).isFalse();
        }
    }

    // ------------------------------------------------------------------
    // Compensation flow
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Compensation flow")
    class CompensationFlow {

        @Test
        void shouldSwitchToCompensatingOnFailure() {
            SagaInstance saga = startSaga();
            saga.markReserved(SagaStepName.FLIGHT);
            saga.failAndStartCompensation(SagaStepName.HOTEL, "No cabins available");

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
            assertThat(saga.isCompensating()).isTrue();
            assertThat(saga.isForwardPhase()).isFalse();
        }

        @Test
        void shouldRecordFailureReasonOnStep() {
            SagaInstance saga = startSaga();
            saga.failAndStartCompensation(SagaStepName.FLIGHT, "Engine failure");

            SagaStep step = saga.getStep(SagaStepName.FLIGHT);
            assertThat(step.getStatus()).isEqualTo(SagaStepStatus.FAILED);
            assertThat(step.getReason()).isEqualTo("Engine failure");
        }

        @Test
        void shouldMarkStepAsCompensated() {
            SagaInstance saga = startSaga();
            saga.markReserved(SagaStepName.FLIGHT);
            saga.failAndStartCompensation(SagaStepName.HOTEL, "fail");
            saga.markCompensated(SagaStepName.FLIGHT);

            assertThat(saga.getStep(SagaStepName.FLIGHT).getStatus())
                    .isEqualTo(SagaStepStatus.COMPENSATED);
        }

        @Test
        void shouldCancelAfterAllCompensated() {
            SagaInstance saga = startSaga();
            saga.markReserved(SagaStepName.FLIGHT);
            saga.failAndStartCompensation(SagaStepName.HOTEL, "fail");
            saga.markCompensated(SagaStepName.FLIGHT);
            saga.cancel();

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.CANCELLED);
        }

        @Test
        void shouldHandleCompensationFailure() {
            SagaInstance saga = startSaga();
            saga.markReserved(SagaStepName.FLIGHT);
            saga.failAndStartCompensation(SagaStepName.HOTEL, "fail");
            saga.markCompensationFailed(SagaStepName.FLIGHT, "Compensation timeout");

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATION_FAILED);
            assertThat(saga.getStep(SagaStepName.FLIGHT).getStatus())
                    .isEqualTo(SagaStepStatus.COMPENSATION_FAILED);
            assertThat(saga.getStep(SagaStepName.FLIGHT).getReason())
                    .isEqualTo("Compensation timeout");
        }
    }

    // ------------------------------------------------------------------
    // getStep()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getStep()")
    class GetStep {

        @Test
        void shouldReturnStepByName() {
            SagaInstance saga = startSaga();

            assertThat(saga.getStep(SagaStepName.PAYMENT))
                    .isNotNull()
                    .satisfies(step -> {
                        assertThat(step.getName()).isEqualTo(SagaStepName.PAYMENT);
                        assertThat(step.getStatus()).isEqualTo(SagaStepStatus.PENDING);
                    });
        }
    }

    // ------------------------------------------------------------------
    // restore()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("restore()")
    class Restore {

        @Test
        void shouldRestoreSagaFromPersistedState() {
            SagaInstance original = startSaga();
            original.markReserved(SagaStepName.FLIGHT);

            SagaInstance restored = SagaInstance.restore(
                    original.getId(),
                    original.getCustomerName(),
                    original.getDestination(),
                    original.getAmount(),
                    original.getStatus(),
                    original.getSteps(),
                    original.getCreatedAt(),
                    original.getUpdatedAt()
            );

            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getStatus()).isEqualTo(original.getStatus());
            assertThat(restored.getStep(SagaStepName.FLIGHT).getStatus())
                    .isEqualTo(SagaStepStatus.RESERVED);
        }
    }

    // ------------------------------------------------------------------
    // Immutability of steps list
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getSteps() should return unmodifiable list")
    void stepsShouldBeUnmodifiable() {
        SagaInstance saga = startSaga();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> saga.getSteps().add(new SagaStep(SagaStepName.FLIGHT)));
    }
}
