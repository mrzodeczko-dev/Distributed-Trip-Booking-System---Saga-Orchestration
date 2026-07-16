package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.event.CommandResult;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.event.SagaParticipantReply;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessage;
import com.rzodeczko.common.outbox.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxSagaReplyPublisherTest {

    @Mock
    private OutboxEventService outboxEventService;

    @Captor
    private ArgumentCaptor<Object> payloadCaptor;

    private OutboxSagaReplyPublisher publisher;

    private static final ParticipantTopologyProperties TOPOLOGY = new ParticipantTopologyProperties(
            "commands-exchange",
            "replies-exchange",
            "dlx-exchange",
            "command-queue",
            "command-dlq",
            "command-routing-key",
            "command-dlq-routing-key",
            "reply-routing-key"
    );

    @BeforeEach
    void setUp() {
        publisher = new OutboxSagaReplyPublisher(outboxEventService, TOPOLOGY);
    }

    @Test
    @DisplayName("should build outbox event with correct type, exchange and routing key")
    void shouldSaveOutboxEventForReply() {
        UUID sagaId = UUID.randomUUID();
        SagaParticipantReply reply = SagaParticipantReply.from(
                sagaId, "HOTEL", SagaAction.RESERVE, CommandResult.success()
        );

        publisher.publish(reply);

        verify(outboxEventService).save(
                eq("HOTEL_RESERVE_REPLY"),
                payloadCaptor.capture(),
                eq("replies-exchange"),
                eq("reply-routing-key")
        );

        Object payload = payloadCaptor.getValue();
        assertThat(payload).isInstanceOf(SagaReplyMessage.class);
        SagaReplyMessage wireMessage = (SagaReplyMessage) payload;
        assertThat(wireMessage.sagaId()).isEqualTo(sagaId);
        assertThat(wireMessage.step()).isEqualTo("HOTEL");
        assertThat(wireMessage.action()).isEqualTo("RESERVE");
        assertThat(wireMessage.status()).isEqualTo("SUCCESS");
        assertThat(wireMessage.reason()).isNull();
    }

    @Test
    @DisplayName("should build correct event type for CANCEL failures")
    void shouldSaveOutboxEventForCancelFailure() {
        UUID sagaId = UUID.randomUUID();
        SagaParticipantReply reply = SagaParticipantReply.from(
                sagaId, "HOTEL", SagaAction.CANCEL, CommandResult.failure("some reason")
        );

        publisher.publish(reply);

        verify(outboxEventService).save(
                eq("HOTEL_CANCEL_REPLY"),
                payloadCaptor.capture(),
                eq("replies-exchange"),
                eq("reply-routing-key")
        );

        SagaReplyMessage wireMessage = (SagaReplyMessage) payloadCaptor.getValue();
        assertThat(wireMessage.status()).isEqualTo("FAILURE");
        assertThat(wireMessage.reason()).isEqualTo("some reason");
    }
}
