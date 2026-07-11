package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.command.FlightCommand;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.port.in.ProcessFlightCommandUseCase;
import com.rzodeczko.infrastructure.messaging.dto.FlightCommandMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Listener komend orkiestratora. Mapuje wiadomosc AMQP na komende aplikacyjna.
 */
@Slf4j
@Component
public class FlightCommandListener {
    private final ProcessFlightCommandUseCase processFlightCommandUseCase;

    public FlightCommandListener(
            @Qualifier("transactionalFlightCommandService")
            ProcessFlightCommandUseCase processFlightCommandUseCase
    ) {
        this.processFlightCommandUseCase = processFlightCommandUseCase;
    }

    @RabbitListener(queues = "${app.rabbitmq.topology.command-queue}")
    public void onCommand(FlightCommandMessageDto message) {
        log.info("[FLIGHT] Received command saga={}, action={}", message.sagaId(), message.action());
        processFlightCommandUseCase.handle(new FlightCommand(
                message.sagaId(),
                SagaAction.valueOf(message.action()),
                message.customerName(),
                message.destination(),
                message.amount()
        ));
    }
}
