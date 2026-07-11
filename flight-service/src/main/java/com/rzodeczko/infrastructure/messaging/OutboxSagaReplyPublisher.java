package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.event.SagaParticipantReply;
import com.rzodeczko.application.port.out.SagaReplyPort;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessageDto;
import com.rzodeczko.infrastructure.outbox.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter portu SagaReplyPort - zapis odpowiedzi do Outboxa (atomowo z operacja biznesowa).
 * OutboxSagaReplyPublisher tłumaczy odpowiedź biznesową na zapis outboxowy, żeby orkiestrator dostał
 * SUCCESS/FAILURE dopiero po trwałym zapisie rezerwacji i idempotencji — zgodnie z czystą architekturą
 * (port + adapter) i wzorcem transactional outbox.
 * Bez tej klasy musiałbyś albo zepsuć warstwy (RabbitMQ w application), albo stracić atomowość
 * (wysyłka poza transakcją).
 */

@Component
@RequiredArgsConstructor
public class OutboxSagaReplyPublisher implements SagaReplyPort {
    private final OutboxEventService outboxEventService;
    private final ParticipantTopologyProperties topology;

    @Override
    public void publish(SagaParticipantReply reply) {
        SagaReplyMessageDto message = new SagaReplyMessageDto(
                reply.sagaId(),
                reply.step(),
                reply.action().name(),
                reply.status(),
                reply.reason()
        );

        outboxEventService.save(
                reply.step() + "_" + reply.action().name() + "_REPLY",
                message,
                topology.repliesExchange(),
                topology.replyRoutingKey()
        );
    }
}
