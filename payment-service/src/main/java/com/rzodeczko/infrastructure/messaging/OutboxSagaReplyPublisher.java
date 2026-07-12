package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.event.SagaParticipantReply;
import com.rzodeczko.application.port.out.SagaReplyPort;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessage;
import com.rzodeczko.infrastructure.outbox.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxSagaReplyPublisher implements SagaReplyPort {

    private final OutboxEventService outboxEventService;
    private final ParticipantTopologyProperties topology;

    @Override
    public void publish(SagaParticipantReply reply) {
        SagaReplyMessage wireMessage = new SagaReplyMessage(
                reply.sagaId(),
                reply.step(),
                reply.action().name(),
                reply.status(),
                reply.reason()
        );
        outboxEventService.save(
                reply.step() + "_" + reply.action().name() + "_REPLY",
                wireMessage,
                topology.repliesExchange(),
                topology.replyRoutingKey()
        );
    }
}
