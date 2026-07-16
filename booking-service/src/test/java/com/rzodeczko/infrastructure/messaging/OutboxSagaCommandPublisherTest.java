package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStepName;
import com.rzodeczko.infrastructure.messaging.dto.ParticipantCommandMessage;
import com.rzodeczko.common.outbox.OutboxEventEntity;
import com.rzodeczko.common.outbox.OutboxEventService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxSagaCommandPublisherTest {

    @Mock
    private OutboxEventService outboxEventService;

    @Captor
    private ArgumentCaptor<Object> payloadCaptor;

    private OutboxSagaCommandPublisher publisher;

    private SagaTopologyProperties topology;

    private static final String CUSTOMER = "Jan Kowalski";
    private static final String DESTINATION = "Mars";
    private static final BigDecimal AMOUNT = new BigDecimal("2500.00");

    @BeforeEach
    void setUp() {
        topology = new SagaTopologyProperties(
                "commands-exchange",
                "replies-exchange",
                "dlx-exchange",
                "reply-queue",
                "reply-dlq",
                "reply-routing-key",
                "reply-dlq-routing-key",
                "flight.reserve",
                "hotel.reserve",
                "payment.reserve"
        );
        publisher = new OutboxSagaCommandPublisher(outboxEventService, topology);
    }

    @Nested
    @DisplayName("sendReserve()")
    class SendReserve {

        @Test
        void shouldCreateOutboxEventWithReserveAction() {
            SagaInstance saga = SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);
            when(outboxEventService.save(any(), any(), any(), any())).thenReturn(new OutboxEventEntity());

            publisher.sendReserve(saga, SagaStepName.FLIGHT);

            verify(outboxEventService).save(
                    eq("FLIGHT_RESERVE"),
                    payloadCaptor.capture(),
                    eq("commands-exchange"),
                    eq("flight.reserve")
            );

            ParticipantCommandMessage payload = (ParticipantCommandMessage) payloadCaptor.getValue();
            assertThat(payload.sagaId()).isEqualTo(saga.getId());
            assertThat(payload.action()).isEqualTo("RESERVE");
            assertThat(payload.customerName()).isEqualTo(CUSTOMER);
            assertThat(payload.destination()).isEqualTo(DESTINATION);
            assertThat(payload.amount()).isEqualByComparingTo(AMOUNT);
        }

        @Test
        void shouldResolveRoutingKeyForHotelStep() {
            SagaInstance saga = SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);
            when(outboxEventService.save(any(), any(), any(), any())).thenReturn(new OutboxEventEntity());

            publisher.sendReserve(saga, SagaStepName.HOTEL);

            verify(outboxEventService).save(eq("HOTEL_RESERVE"), any(), eq("commands-exchange"), eq("hotel.reserve"));
        }

        @Test
        void shouldResolveRoutingKeyForPaymentStep() {
            SagaInstance saga = SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);
            when(outboxEventService.save(any(), any(), any(), any())).thenReturn(new OutboxEventEntity());

            publisher.sendReserve(saga, SagaStepName.PAYMENT);

            verify(outboxEventService).save(eq("PAYMENT_RESERVE"), any(), eq("commands-exchange"), eq("payment.reserve"));
        }
    }

    @Nested
    @DisplayName("sendCancel()")
    class SendCancel {

        @Test
        void shouldCreateOutboxEventWithCancelAction() {
            SagaInstance saga = SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);
            when(outboxEventService.save(any(), any(), any(), any())).thenReturn(new OutboxEventEntity());

            publisher.sendCancel(saga, SagaStepName.FLIGHT);

            verify(outboxEventService).save(
                    eq("FLIGHT_CANCEL"),
                    payloadCaptor.capture(),
                    eq("commands-exchange"),
                    eq("flight.reserve")
            );

            ParticipantCommandMessage payload = (ParticipantCommandMessage) payloadCaptor.getValue();
            assertThat(payload.action()).isEqualTo("CANCEL");
            assertThat(payload.sagaId()).isEqualTo(saga.getId());
        }
    }
}
