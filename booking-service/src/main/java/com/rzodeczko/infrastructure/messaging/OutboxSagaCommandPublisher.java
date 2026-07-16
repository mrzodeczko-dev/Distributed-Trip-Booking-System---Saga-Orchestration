package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.port.out.SagaCommandPort;
import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStepName;
import com.rzodeczko.infrastructure.messaging.dto.ParticipantCommandMessage;
import com.rzodeczko.common.outbox.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxSagaCommandPublisher implements SagaCommandPort {

    private final OutboxEventService outboxEventService;
    private final SagaTopologyProperties topology;

    @Override
    public void sendReserve(SagaInstance saga, SagaStepName step) {
        enqueue(saga, step, "RESERVE");
    }

    @Override
    public void sendCancel(SagaInstance saga, SagaStepName step) {
        enqueue(saga, step, "CANCEL");
    }

    private void enqueue(SagaInstance saga, SagaStepName step, String action) {
        ParticipantCommandMessage command = new ParticipantCommandMessage(
                saga.getId(), action, saga.getCustomerName(), saga.getDestination(), saga.getAmount()
        );
        outboxEventService.save(
                step.name() + "_" + action,
                command,
                topology.commandsExchange(),
                routingKeyFor(step)
        );
    }

    private String routingKeyFor(SagaStepName step) {
        return switch (step) {
            case FLIGHT -> topology.flightCommandRoutingKey();
            case HOTEL -> topology.hotelCommandRoutingKey();
            case PAYMENT -> topology.paymentCommandRoutingKey();
        };
    }
}
