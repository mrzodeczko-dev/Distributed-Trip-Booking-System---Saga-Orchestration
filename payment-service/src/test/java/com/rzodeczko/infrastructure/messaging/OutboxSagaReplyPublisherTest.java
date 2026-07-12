package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.event.CommandResult;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.event.SagaParticipantReply;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessage;
import com.rzodeczko.infrastructure.outbox.OutboxEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxSagaReplyPublisher")
class OutboxSagaReplyPublisherTest {

    @Mock
    private OutboxEventService outboxEventService;

    private final ParticipantTopologyProperties topology = new ParticipantTopologyProperties(
            "commands.exchange",
            "replies.exchange",
            "dlx.exchange",
            "command.queue",
            "command.dlq",
            "command.routing.key",
            "command.dlq.routing.key",
            "reply.routing.key"
    );

    @Test
    @DisplayName("builds a SagaReplyMessage and saves it to the outbox with the correct type/exchange/routing key")
    void publishesReplyThroughOutbox() {
        OutboxSagaReplyPublisher publisher = new OutboxSagaReplyPublisher(outboxEventService, topology);
        UUID sagaId = UUID.randomUUID();
        SagaParticipantReply reply = SagaParticipantReply.from(
                sagaId, "PAYMENT", SagaAction.RESERVE, CommandResult.success());

        publisher.publish(reply);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxEventService).save(
                eq("PAYMENT_RESERVE_REPLY"),
                payloadCaptor.capture(),
                eq("replies.exchange"),
                eq("reply.routing.key")
        );

        assertThat(payloadCaptor.getValue()).isInstanceOf(SagaReplyMessage.class);
        SagaReplyMessage wireMessage = (SagaReplyMessage) payloadCaptor.getValue();
        assertThat(wireMessage.sagaId()).isEqualTo(sagaId);
        assertThat(wireMessage.step()).isEqualTo("PAYMENT");
        assertThat(wireMessage.action()).isEqualTo("RESERVE");
        assertThat(wireMessage.status()).isEqualTo("SUCCESS");
        assertThat(wireMessage.reason()).isNull();
    }

    @Test
    @DisplayName("builds the correct event type for a CANCEL failure reply")
    void publishesCancelFailureReply() {
        OutboxSagaReplyPublisher publisher = new OutboxSagaReplyPublisher(outboxEventService, topology);
        UUID sagaId = UUID.randomUUID();
        SagaParticipantReply reply = SagaParticipantReply.from(
                sagaId, "PAYMENT", SagaAction.CANCEL, CommandResult.failure("some reason"));

        publisher.publish(reply);

        verify(outboxEventService).save(
                eq("PAYMENT_CANCEL_REPLY"),
                any(SagaReplyMessage.class),
                eq("replies.exchange"),
                eq("reply.routing.key")
        );
    }
}
