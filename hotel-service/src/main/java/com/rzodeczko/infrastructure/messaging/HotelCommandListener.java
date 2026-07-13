package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.command.HotelCommand;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.port.in.ProcessHotelCommandUseCase;
import com.rzodeczko.infrastructure.messaging.dto.HotelCommandMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HotelCommandListener {
    private final ProcessHotelCommandUseCase processHotelCommandUseCase;

    public HotelCommandListener(
            @Qualifier("transactionalHotelCommandService")
            ProcessHotelCommandUseCase processHotelCommandUseCase) {
        this.processHotelCommandUseCase = processHotelCommandUseCase;
    }

    @RabbitListener(queues = "${app.rabbitmq.topology.command-queue}")
    public void onCommand(HotelCommandMessage message) {
        log.info("[HOTEL] Received command action={}", message.action());
        processHotelCommandUseCase.handle(new HotelCommand(
                message.sagaId(),
                SagaAction.valueOf(message.action()),
                message.customerName(),
                message.destination(),
                message.amount()
        ));
    }
}
