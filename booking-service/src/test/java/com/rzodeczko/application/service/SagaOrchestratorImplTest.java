package com.rzodeczko.application.service;

import com.rzodeczko.application.command.StartTripBookingCommand;
import com.rzodeczko.application.dto.SagaInstanceDto;
import com.rzodeczko.application.event.ReplyStatus;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.event.SagaReply;
import com.rzodeczko.application.port.out.SagaCommandPort;
import com.rzodeczko.application.port.out.SagaInstanceRepository;
import com.rzodeczko.domain.exception.SagaNotFoundException;
import com.rzodeczko.domain.model.saga.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaOrchestratorImplTest {

    @Mock
    private SagaInstanceRepository sagaInstanceRepository;

    @Mock
    private SagaCommandPort sagaCommandPort;

    @Captor
    private ArgumentCaptor<SagaInstance> sagaCaptor;

    private SagaOrchestratorImpl orchestrator;

    private static final String CUSTOMER = "Jan Kowalski";
    private static final String DESTINATION = "Mars";
    private static final BigDecimal AMOUNT = new BigDecimal("2500.00");

    @BeforeEach
    void setUp() {
        orchestrator = new SagaOrchestratorImpl(sagaInstanceRepository, sagaCommandPort);
    }

    // ------------------------------------------------------------------
    // start()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("start()")
    class StartBooking {

        @Test
        void shouldCreateSagaAndSendFirstCommand() {
            StartTripBookingCommand command = new StartTripBookingCommand(CUSTOMER, DESTINATION, AMOUNT);

            SagaInstanceDto result = orchestrator.start(command);

            assertThat(result.customerName()).isEqualTo(CUSTOMER);
            assertThat(result.destination()).isEqualTo(DESTINATION);
            assertThat(result.amount()).isEqualByComparingTo(AMOUNT);
            assertThat(result.status()).isEqualTo("IN_PROGRESS");

            verify(sagaInstanceRepository).save(any(SagaInstance.class));
            verify(sagaCommandPort).sendReserve(any(SagaInstance.class), eq(SagaStepName.FLIGHT));
        }

        @Test
        void shouldSaveBeforeSendingCommand() {
            StartTripBookingCommand command = new StartTripBookingCommand(CUSTOMER, DESTINATION, AMOUNT);

            orchestrator.start(command);

            var order = inOrder(sagaInstanceRepository, sagaCommandPort);
            order.verify(sagaInstanceRepository).save(any(SagaInstance.class));
            order.verify(sagaCommandPort).sendReserve(any(SagaInstance.class), eq(SagaStepName.FLIGHT));
        }
    }

    // ------------------------------------------------------------------
    // handle() — forward phase
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("handle() — forward phase")
    class HandleForwardReply {

        @Test
        void shouldAdvanceToNextStepOnSuccess() {
            SagaInstance saga = sagaInProgress();
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            SagaReply reply = reserveSuccess(saga.getId(), SagaStepName.FLIGHT);
            orchestrator.handle(reply);

            verify(sagaInstanceRepository).save(sagaCaptor.capture());
            SagaInstance saved = sagaCaptor.getValue();
            assertThat(saved.getStep(SagaStepName.FLIGHT).getStatus()).isEqualTo(SagaStepStatus.RESERVED);

            verify(sagaCommandPort).sendReserve(any(), eq(SagaStepName.HOTEL));
        }

        @Test
        void shouldCompleteWhenLastStepSucceeds() {
            SagaInstance saga = sagaWithStepsReserved(SagaStepName.FLIGHT, SagaStepName.HOTEL);
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            SagaReply reply = reserveSuccess(saga.getId(), SagaStepName.PAYMENT);
            orchestrator.handle(reply);

            verify(sagaInstanceRepository, times(1)).save(sagaCaptor.capture());
            SagaInstance saved = sagaCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(SagaStatus.COMPLETED);

            verify(sagaCommandPort, never()).sendReserve(any(), any());
        }

        @Test
        void shouldStartCompensationOnFailure() {
            SagaInstance saga = sagaWithStepsReserved(SagaStepName.FLIGHT);
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            SagaReply reply = reserveFailure(saga.getId(), SagaStepName.HOTEL, "No cabins");
            orchestrator.handle(reply);

            verify(sagaInstanceRepository).save(sagaCaptor.capture());
            SagaInstance saved = sagaCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
            assertThat(saved.getStep(SagaStepName.HOTEL).getStatus()).isEqualTo(SagaStepStatus.FAILED);
            assertThat(saved.getStep(SagaStepName.HOTEL).getReason()).isEqualTo("No cabins");

            verify(sagaCommandPort).sendCancel(any(), eq(SagaStepName.FLIGHT));
        }

        @Test
        void shouldCancelDirectlyWhenFailureAndNothingToCompensate() {
            SagaInstance saga = sagaInProgress();
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            SagaReply reply = reserveFailure(saga.getId(), SagaStepName.FLIGHT, "No seats");
            orchestrator.handle(reply);

            verify(sagaInstanceRepository).save(sagaCaptor.capture());
            SagaInstance saved = sagaCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(SagaStatus.CANCELLED);

            verify(sagaCommandPort, never()).sendCancel(any(), any());
        }

        @Test
        void shouldIgnoreDuplicateReserveReply() {
            SagaInstance saga = sagaWithStepsReserved(SagaStepName.FLIGHT);
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            // Duplicate: FLIGHT already RESERVED
            SagaReply reply = reserveSuccess(saga.getId(), SagaStepName.FLIGHT);
            orchestrator.handle(reply);

            verify(sagaInstanceRepository, never()).save(any());
            verify(sagaCommandPort, never()).sendReserve(any(), any());
        }

        @Test
        void shouldIgnoreCancelActionInForwardPhase() {
            SagaInstance saga = sagaInProgress();
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            SagaReply reply = new SagaReply(
                    saga.getId(), SagaStepName.FLIGHT, SagaAction.CANCEL, ReplyStatus.SUCCESS, null
            );
            orchestrator.handle(reply);

            verify(sagaInstanceRepository, never()).save(any());
            verify(sagaCommandPort, never()).sendCancel(any(), any());
        }
    }

    // ------------------------------------------------------------------
    // handle() — compensation phase
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("handle() — compensation phase")
    class HandleCompensationReply {

        @Test
        void shouldCompensateNextStepOnSuccess() {
            SagaInstance saga = sagaCompensating(SagaStepName.FLIGHT, SagaStepName.HOTEL);
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            SagaReply reply = cancelSuccess(saga.getId(), SagaStepName.HOTEL);
            orchestrator.handle(reply);

            verify(sagaCommandPort).sendCancel(any(), eq(SagaStepName.FLIGHT));
            verify(sagaInstanceRepository).save(sagaCaptor.capture());
            assertThat(sagaCaptor.getValue().getStep(SagaStepName.HOTEL).getStatus())
                    .isEqualTo(SagaStepStatus.COMPENSATED);
        }

        @Test
        void shouldCancelWhenAllStepsCompensated() {
            SagaInstance saga = sagaCompensatingOnlyFlight();
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            SagaReply reply = cancelSuccess(saga.getId(), SagaStepName.FLIGHT);
            orchestrator.handle(reply);

            verify(sagaInstanceRepository).save(sagaCaptor.capture());
            assertThat(sagaCaptor.getValue().getStatus()).isEqualTo(SagaStatus.CANCELLED);

            verify(sagaCommandPort, never()).sendCancel(any(), any());
        }

        @Test
        void shouldMarkCompensationFailedOnFailure() {
            SagaInstance saga = sagaCompensatingOnlyFlight();
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            SagaReply reply = cancelFailure(saga.getId(), SagaStepName.FLIGHT, "Timeout");
            orchestrator.handle(reply);

            verify(sagaInstanceRepository).save(sagaCaptor.capture());
            SagaInstance saved = sagaCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(SagaStatus.COMPENSATION_FAILED);
            assertThat(saved.getStep(SagaStepName.FLIGHT).getReason()).isEqualTo("Timeout");
        }

        @Test
        void shouldIgnoreDuplicateCompensationReply() {
            SagaInstance saga = sagaCompensating(SagaStepName.FLIGHT, SagaStepName.HOTEL);
            saga.markCompensated(SagaStepName.HOTEL);
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            SagaReply reply = cancelSuccess(saga.getId(), SagaStepName.HOTEL);
            orchestrator.handle(reply);

            verify(sagaInstanceRepository, never()).save(any());
        }

        @Test
        void shouldIgnoreReserveActionInCompensationPhase() {
            SagaInstance saga = sagaCompensatingOnlyFlight();
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            SagaReply reply = reserveSuccess(saga.getId(), SagaStepName.FLIGHT);
            orchestrator.handle(reply);

            verify(sagaInstanceRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------
    // handle() — terminal state
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("handle() — terminal states")
    class HandleTerminalState {

        @Test
        void shouldIgnoreReplyForCompletedSaga() {
            SagaInstance saga = completedSaga();
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            SagaReply reply = reserveSuccess(saga.getId(), SagaStepName.FLIGHT);
            orchestrator.handle(reply);

            verify(sagaInstanceRepository, never()).save(any());
            verify(sagaCommandPort, never()).sendReserve(any(), any());
        }

        @Test
        void shouldThrowWhenSagaNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(sagaInstanceRepository.findByIdForUpdate(unknownId)).thenReturn(Optional.empty());

            SagaReply reply = reserveSuccess(unknownId, SagaStepName.FLIGHT);

            assertThatExceptionOfType(SagaNotFoundException.class)
                    .isThrownBy(() -> orchestrator.handle(reply));
        }
    }

    // ------------------------------------------------------------------
    // Full scenario tests
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Full scenarios")
    class FullScenarios {

        @Test
        void happyPath_allThreeStepsSucceed() {
            // Start saga
            StartTripBookingCommand command = new StartTripBookingCommand(CUSTOMER, DESTINATION, AMOUNT);
            SagaInstanceDto dto = orchestrator.start(command);
            UUID sagaId = UUID.fromString(dto.sagaId());

            // Capture the saga saved during start
            verify(sagaInstanceRepository).save(sagaCaptor.capture());
            SagaInstance saga = sagaCaptor.getValue();

            // Simulate: FLIGHT success
            when(sagaInstanceRepository.findByIdForUpdate(sagaId)).thenReturn(Optional.of(saga));
            orchestrator.handle(reserveSuccess(sagaId, SagaStepName.FLIGHT));

            // Simulate: HOTEL success
            orchestrator.handle(reserveSuccess(sagaId, SagaStepName.HOTEL));

            // Simulate: PAYMENT success
            orchestrator.handle(reserveSuccess(sagaId, SagaStepName.PAYMENT));

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
            assertThat(saga.getSteps()).allSatisfy(step ->
                    assertThat(step.getStatus()).isEqualTo(SagaStepStatus.RESERVED)
            );
        }

        @Test
        void compensationPath_hotelFailsThenFlightCompensated() {
            SagaInstance saga = sagaWithStepsReserved(SagaStepName.FLIGHT);
            when(sagaInstanceRepository.findByIdForUpdate(saga.getId())).thenReturn(Optional.of(saga));

            // HOTEL fails → compensation starts
            orchestrator.handle(reserveFailure(saga.getId(), SagaStepName.HOTEL, "No cabins"));
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATING);

            // FLIGHT compensated → saga cancelled
            orchestrator.handle(cancelSuccess(saga.getId(), SagaStepName.FLIGHT));
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.CANCELLED);

            assertThat(saga.getStep(SagaStepName.FLIGHT).getStatus()).isEqualTo(SagaStepStatus.COMPENSATED);
            assertThat(saga.getStep(SagaStepName.HOTEL).getStatus()).isEqualTo(SagaStepStatus.FAILED);
            assertThat(saga.getStep(SagaStepName.PAYMENT).getStatus()).isEqualTo(SagaStepStatus.PENDING);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private SagaInstance sagaInProgress(SagaStepName... reservedSteps) {
        SagaInstance saga = SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);
        for (SagaStepName step : reservedSteps) {
            saga.markReserved(step);
        }
        return saga;
    }

    private SagaInstance sagaInProgress() {
        return SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);
    }

    private SagaInstance sagaWithStepsReserved(SagaStepName... steps) {
        SagaInstance saga = SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);
        for (SagaStepName step : steps) {
            saga.markReserved(step);
        }
        return saga;
    }

    private SagaInstance sagaCompensating(SagaStepName... reservedSteps) {
        SagaInstance saga = sagaWithStepsReserved(reservedSteps);
        SagaStepName failedStep = reservedSteps[reservedSteps.length - 1].next()
                .orElse(SagaStepName.PAYMENT);
        saga.failAndStartCompensation(failedStep, "test failure");
        return saga;
    }

    private SagaInstance sagaCompensatingOnlyFlight() {
        SagaInstance saga = sagaWithStepsReserved(SagaStepName.FLIGHT);
        saga.failAndStartCompensation(SagaStepName.HOTEL, "test failure");
        return saga;
    }

    private SagaInstance completedSaga() {
        SagaInstance saga = sagaWithStepsReserved(
                SagaStepName.FLIGHT, SagaStepName.HOTEL, SagaStepName.PAYMENT
        );
        saga.complete();
        return saga;
    }

    private SagaReply reserveSuccess(UUID sagaId, SagaStepName step) {
        return new SagaReply(sagaId, step, SagaAction.RESERVE, ReplyStatus.SUCCESS, null);
    }

    private SagaReply reserveFailure(UUID sagaId, SagaStepName step, String reason) {
        return new SagaReply(sagaId, step, SagaAction.RESERVE, ReplyStatus.FAILURE, reason);
    }

    private SagaReply cancelSuccess(UUID sagaId, SagaStepName step) {
        return new SagaReply(sagaId, step, SagaAction.CANCEL, ReplyStatus.SUCCESS, null);
    }

    private SagaReply cancelFailure(UUID sagaId, SagaStepName step, String reason) {
        return new SagaReply(sagaId, step, SagaAction.CANCEL, ReplyStatus.FAILURE, reason);
    }
}
