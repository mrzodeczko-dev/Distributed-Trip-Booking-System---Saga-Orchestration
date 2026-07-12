package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.event.SagaParticipantReply;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessageDto;
import com.rzodeczko.infrastructure.outbox.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxSagaReplyPublisherTest {

    @Mock
    private OutboxEventService outboxEventService;

    @Captor
    private ArgumentCaptor<Object> payloadCaptor;

    private OutboxSagaReplyPublisher publisher;

    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final ParticipantTopologyProperties TOPOLOGY = new ParticipantTopologyProperties(
            "x.saga.commands",
            "x.saga.replies",
            "x.saga.dlx",
            "q.flight-service.commands",
            "q.flight-service.commands.dlq",
            "flight.cmd",
            "flight.cmd.dlq",
            "flight.reply"
    );

    @BeforeEach
    void setUp() {
        publisher = new OutboxSagaReplyPublisher(outboxEventService, TOPOLOGY);
    }

    @Nested
    @DisplayName("publish")
    class Publish {

        @Test
        void shouldSaveOutboxEventWithExpectedTypeAndRouting() {
            SagaParticipantReply reply = new SagaParticipantReply(SAGA_ID, "FLIGHT", SagaAction.RESERVE, "SUCCESS", null);

            publisher.publish(reply);

            verify(outboxEventService).save(
                    org.mockito.ArgumentMatchers.eq("FLIGHT_RESERVE_REPLY"),
                    payloadCaptor.capture(),
                    org.mockito.ArgumentMatchers.eq(TOPOLOGY.repliesExchange()),
                    org.mockito.ArgumentMatchers.eq(TOPOLOGY.replyRoutingKey())
            );

            assertThat(payloadCaptor.getValue()).isInstanceOf(SagaReplyMessageDto.class);
            SagaReplyMessageDto dto = (SagaReplyMessageDto) payloadCaptor.getValue();
            assertThat(dto.sagaId()).isEqualTo(SAGA_ID);
            assertThat(dto.step()).isEqualTo("FLIGHT");
            assertThat(dto.action()).isEqualTo("RESERVE");
            assertThat(dto.status()).isEqualTo("SUCCESS");
            assertThat(dto.reason()).isNull();
        }

        @Test
        void shouldBuildEventTypeForCancelFailure() {
            SagaParticipantReply reply = new SagaParticipantReply(SAGA_ID, "FLIGHT", SagaAction.CANCEL, "FAILURE", "no seats");

            publisher.publish(reply);

            verify(outboxEventService).save(
                    org.mockito.ArgumentMatchers.eq("FLIGHT_CANCEL_REPLY"),
                    any(),
                    any(),
                    any()
            );
        }
    }
}
