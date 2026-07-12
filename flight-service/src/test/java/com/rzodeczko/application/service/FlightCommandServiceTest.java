package com.rzodeczko.application.service;

import com.rzodeczko.application.command.FlightCommand;
import com.rzodeczko.application.event.CommandResult;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.event.SagaParticipantReply;
import com.rzodeczko.application.port.out.ProcessedMessageStore;
import com.rzodeczko.application.port.out.SagaReplyPort;
import com.rzodeczko.application.port.out.SeatReservationRepository;
import com.rzodeczko.domain.model.ReservationStatus;
import com.rzodeczko.domain.model.SeatReservation;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightCommandServiceTest {

    @Mock
    private ProcessedMessageStore processedMessageStore;

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @Mock
    private SagaReplyPort sagaReplyPort;

    @Captor
    private ArgumentCaptor<SagaParticipantReply> replyCaptor;

    @Captor
    private ArgumentCaptor<SeatReservation> reservationCaptor;

    private FlightCommandService service;

    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final String CUSTOMER = "Jan Kowalski";
    private static final String DESTINATION = "Mars";
    private static final BigDecimal AMOUNT = new BigDecimal("1500.00");

    @BeforeEach
    void setUp() {
        service = new FlightCommandService(processedMessageStore, seatReservationRepository, sagaReplyPort);
    }

    // ------------------------------------------------------------------
    // Idempotency
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Idempotency")
    class Idempotency {

        @Test
        void shouldSkipDuplicateCommand() {
            when(processedMessageStore.existsByMessageKey(SAGA_ID + ":RESERVE")).thenReturn(true);

            service.handle(reserveCommand());

            verify(sagaReplyPort, never()).publish(any());
            verify(seatReservationRepository, never()).save(any());
            verify(processedMessageStore, never()).markProcessed(any());
        }
    }

    // ------------------------------------------------------------------
    // RESERVE
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("RESERVE action")
    class ReserveAction {

        @Test
        void shouldCreateReservationAndPublishSuccess() {
            when(processedMessageStore.existsByMessageKey(any())).thenReturn(false);
            when(seatReservationRepository.existsBySagaId(SAGA_ID)).thenReturn(false);

            service.handle(reserveCommand());

            verify(seatReservationRepository).save(reservationCaptor.capture());
            SeatReservation saved = reservationCaptor.getValue();
            assertThat(saved.getSagaId()).isEqualTo(SAGA_ID);
            assertThat(saved.getCustomerName()).isEqualTo(CUSTOMER);
            assertThat(saved.getDestination()).isEqualTo(DESTINATION);
            assertThat(saved.getStatus()).isEqualTo(ReservationStatus.RESERVED);

            verify(processedMessageStore).markProcessed(SAGA_ID + ":RESERVE");

            verify(sagaReplyPort).publish(replyCaptor.capture());
            SagaParticipantReply reply = replyCaptor.getValue();
            assertThat(reply.sagaId()).isEqualTo(SAGA_ID);
            assertThat(reply.step()).isEqualTo("FLIGHT");
            assertThat(reply.action()).isEqualTo(SagaAction.RESERVE);
            assertThat(reply.status()).isEqualTo("SUCCESS");
            assertThat(reply.reason()).isNull();
        }

        @Test
        void shouldReturnSuccessIfReservationAlreadyExists() {
            when(processedMessageStore.existsByMessageKey(any())).thenReturn(false);
            when(seatReservationRepository.existsBySagaId(SAGA_ID)).thenReturn(true);

            service.handle(reserveCommand());

            verify(seatReservationRepository, never()).save(any());

            verify(sagaReplyPort).publish(replyCaptor.capture());
            assertThat(replyCaptor.getValue().status()).isEqualTo("SUCCESS");
        }

        @Test
        void shouldPublishFailureForBlockedCustomer() {
            when(processedMessageStore.existsByMessageKey(any())).thenReturn(false);
            when(seatReservationRepository.existsBySagaId(SAGA_ID)).thenReturn(false);

            FlightCommand blockedCommand = new FlightCommand(
                    SAGA_ID, SagaAction.RESERVE, "BLOCKED_USER", DESTINATION, AMOUNT
            );
            service.handle(blockedCommand);

            verify(seatReservationRepository, never()).save(any());

            verify(sagaReplyPort).publish(replyCaptor.capture());
            SagaParticipantReply reply = replyCaptor.getValue();
            assertThat(reply.status()).isEqualTo("FAILURE");
            assertThat(reply.reason()).isNotBlank();
        }

        @Test
        void shouldMarkMessageProcessedBeforePublishing() {
            when(processedMessageStore.existsByMessageKey(any())).thenReturn(false);
            when(seatReservationRepository.existsBySagaId(SAGA_ID)).thenReturn(false);

            service.handle(reserveCommand());

            var order = inOrder(processedMessageStore, sagaReplyPort);
            order.verify(processedMessageStore).markProcessed(any());
            order.verify(sagaReplyPort).publish(any());
        }
    }

    // ------------------------------------------------------------------
    // CANCEL
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("CANCEL action")
    class CancelAction {

        @Test
        void shouldCancelExistingReservationAndPublishSuccess() {
            when(processedMessageStore.existsByMessageKey(any())).thenReturn(false);
            SeatReservation reservation = SeatReservation.reserve(SAGA_ID, CUSTOMER, DESTINATION);
            when(seatReservationRepository.findBySagaId(SAGA_ID)).thenReturn(Optional.of(reservation));

            service.handle(cancelCommand());

            verify(seatReservationRepository).save(reservationCaptor.capture());
            assertThat(reservationCaptor.getValue().getStatus()).isEqualTo(ReservationStatus.CANCELLED);

            verify(sagaReplyPort).publish(replyCaptor.capture());
            SagaParticipantReply reply = replyCaptor.getValue();
            assertThat(reply.action()).isEqualTo(SagaAction.CANCEL);
            assertThat(reply.status()).isEqualTo("SUCCESS");
        }

        @Test
        void shouldPublishSuccessEvenWhenNoReservationExists() {
            when(processedMessageStore.existsByMessageKey(any())).thenReturn(false);
            when(seatReservationRepository.findBySagaId(SAGA_ID)).thenReturn(Optional.empty());

            service.handle(cancelCommand());

            verify(seatReservationRepository, never()).save(any());

            verify(sagaReplyPort).publish(replyCaptor.capture());
            assertThat(replyCaptor.getValue().status()).isEqualTo("SUCCESS");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private FlightCommand reserveCommand() {
        return new FlightCommand(SAGA_ID, SagaAction.RESERVE, CUSTOMER, DESTINATION, AMOUNT);
    }

    private FlightCommand cancelCommand() {
        return new FlightCommand(SAGA_ID, SagaAction.CANCEL, CUSTOMER, DESTINATION, AMOUNT);
    }
}
