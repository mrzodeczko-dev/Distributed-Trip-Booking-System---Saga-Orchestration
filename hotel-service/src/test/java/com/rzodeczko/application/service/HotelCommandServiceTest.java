package com.rzodeczko.application.service;

import com.rzodeczko.application.command.HotelCommand;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.event.SagaParticipantReply;
import com.rzodeczko.application.port.out.CabinReservationRepository;
import com.rzodeczko.application.port.out.ProcessedMessageStore;
import com.rzodeczko.application.port.out.SagaReplyPort;
import com.rzodeczko.domain.model.CabinReservation;
import com.rzodeczko.domain.model.ReservationStatus;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelCommandServiceTest {

    @Mock
    private ProcessedMessageStore processedMessageStore;

    @Mock
    private CabinReservationRepository cabinReservationRepository;

    @Mock
    private SagaReplyPort sagaReplyPort;

    @Captor
    private ArgumentCaptor<SagaParticipantReply> replyCaptor;

    @Captor
    private ArgumentCaptor<CabinReservation> reservationCaptor;

    private HotelCommandService service;

    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final String CUSTOMER = "Jan Kowalski";
    private static final String DESTINATION = "Venus";
    private static final BigDecimal AMOUNT = new BigDecimal("999.99");

    @BeforeEach
    void setUp() {
        service = new HotelCommandService(processedMessageStore, cabinReservationRepository, sagaReplyPort);
    }

    @Nested
    @DisplayName("Idempotency")
    class Idempotency {

        @Test
        void shouldSkipDuplicateCommand() {
            when(processedMessageStore.existsByMessageKey(SAGA_ID + ":RESERVE")).thenReturn(true);

            service.handle(reserveCommand());

            verify(sagaReplyPort, never()).publish(any());
            verify(cabinReservationRepository, never()).save(any());
            verify(processedMessageStore, never()).markAsProcessed(any());
        }

        @Test
        void shouldSkipDuplicateCancelCommand() {
            when(processedMessageStore.existsByMessageKey(SAGA_ID + ":CANCEL")).thenReturn(true);

            service.handle(cancelCommand());

            verify(sagaReplyPort, never()).publish(any());
            verify(cabinReservationRepository, never()).findBySagaId(any());
        }
    }

    @Nested
    @DisplayName("RESERVE action")
    class ReserveAction {

        @Test
        void shouldCreateReservationAndPublishSuccess() {
            when(processedMessageStore.existsByMessageKey(any())).thenReturn(false);
            when(cabinReservationRepository.existsBySagaId(SAGA_ID)).thenReturn(false);

            service.handle(reserveCommand());

            verify(cabinReservationRepository).save(reservationCaptor.capture());
            CabinReservation saved = reservationCaptor.getValue();
            assertThat(saved.getSagaId()).isEqualTo(SAGA_ID);
            assertThat(saved.getCustomerName()).isEqualTo(CUSTOMER);
            assertThat(saved.getDestination()).isEqualTo(DESTINATION);
            assertThat(saved.getStatus()).isEqualTo(ReservationStatus.RESERVED);

            verify(processedMessageStore).markAsProcessed(SAGA_ID + ":RESERVE");

            verify(sagaReplyPort).publish(replyCaptor.capture());
            SagaParticipantReply reply = replyCaptor.getValue();
            assertThat(reply.sagaId()).isEqualTo(SAGA_ID);
            assertThat(reply.step()).isEqualTo("HOTEL");
            assertThat(reply.action()).isEqualTo(SagaAction.RESERVE);
            assertThat(reply.status()).isEqualTo("SUCCESS");
            assertThat(reply.reason()).isNull();
        }

        @Test
        void shouldReturnSuccessIfReservationAlreadyExists() {
            when(processedMessageStore.existsByMessageKey(any())).thenReturn(false);
            when(cabinReservationRepository.existsBySagaId(SAGA_ID)).thenReturn(true);

            service.handle(reserveCommand());

            verify(cabinReservationRepository, never()).save(any());

            verify(sagaReplyPort).publish(replyCaptor.capture());
            assertThat(replyCaptor.getValue().status()).isEqualTo("SUCCESS");
        }

        @Test
        void shouldPublishFailureForMarsDestination() {
            when(processedMessageStore.existsByMessageKey(any())).thenReturn(false);
            when(cabinReservationRepository.existsBySagaId(SAGA_ID)).thenReturn(false);

            HotelCommand blockedCommand = new HotelCommand(SAGA_ID, SagaAction.RESERVE, CUSTOMER, "MARS", AMOUNT);
            service.handle(blockedCommand);

            verify(cabinReservationRepository, never()).save(any());

            verify(sagaReplyPort).publish(replyCaptor.capture());
            SagaParticipantReply reply = replyCaptor.getValue();
            assertThat(reply.status()).isEqualTo("FAILURE");
            assertThat(reply.reason()).isNotBlank();

            verify(processedMessageStore).markAsProcessed(SAGA_ID + ":RESERVE");
        }

        @Test
        void shouldMarkMessageProcessedBeforePublishing() {
            when(processedMessageStore.existsByMessageKey(any())).thenReturn(false);
            when(cabinReservationRepository.existsBySagaId(SAGA_ID)).thenReturn(false);

            service.handle(reserveCommand());

            var order = inOrder(processedMessageStore, sagaReplyPort);
            order.verify(processedMessageStore).markAsProcessed(any());
            order.verify(sagaReplyPort).publish(any());
        }
    }

    @Nested
    @DisplayName("CANCEL action")
    class CancelAction {

        @Test
        void shouldCancelExistingReservationAndPublishSuccess() {
            when(processedMessageStore.existsByMessageKey(any())).thenReturn(false);
            CabinReservation reservation = CabinReservation.reserve(SAGA_ID, CUSTOMER, DESTINATION);
            when(cabinReservationRepository.findBySagaId(SAGA_ID)).thenReturn(Optional.of(reservation));

            service.handle(cancelCommand());

            verify(cabinReservationRepository).save(reservationCaptor.capture());
            assertThat(reservationCaptor.getValue().getStatus()).isEqualTo(ReservationStatus.CANCELLED);

            verify(sagaReplyPort).publish(replyCaptor.capture());
            SagaParticipantReply reply = replyCaptor.getValue();
            assertThat(reply.action()).isEqualTo(SagaAction.CANCEL);
            assertThat(reply.status()).isEqualTo("SUCCESS");

            verify(processedMessageStore).markAsProcessed(SAGA_ID + ":CANCEL");
        }

        @Test
        void shouldPublishSuccessEvenWhenNoReservationExists() {
            when(processedMessageStore.existsByMessageKey(any())).thenReturn(false);
            when(cabinReservationRepository.findBySagaId(SAGA_ID)).thenReturn(Optional.empty());

            service.handle(cancelCommand());

            verify(cabinReservationRepository, never()).save(any());

            verify(sagaReplyPort).publish(replyCaptor.capture());
            assertThat(replyCaptor.getValue().status()).isEqualTo("SUCCESS");
        }
    }

    private HotelCommand reserveCommand() {
        return new HotelCommand(SAGA_ID, SagaAction.RESERVE, CUSTOMER, DESTINATION, AMOUNT);
    }

    private HotelCommand cancelCommand() {
        return new HotelCommand(SAGA_ID, SagaAction.CANCEL, CUSTOMER, DESTINATION, AMOUNT);
    }
}
