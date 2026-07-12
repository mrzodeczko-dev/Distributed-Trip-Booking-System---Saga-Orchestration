package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.command.PaymentCommand;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.port.in.ProcessPaymentCommandUseCase;
import com.rzodeczko.infrastructure.messaging.dto.PaymentCommandMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentCommandListener {

    private final ProcessPaymentCommandUseCase processPaymentCommandUseCase;

    public PaymentCommandListener(
            @Qualifier("transactionalPaymentCommandService")
            ProcessPaymentCommandUseCase processPaymentCommandUseCase
    ) {
        this.processPaymentCommandUseCase = processPaymentCommandUseCase;
    }

    @RabbitListener(queues = "${app.rabbitmq.topology.command-queue}")
    public void onCommand(PaymentCommandMessage message) {
        log.info("[PAYMENT] Received command saga={}, action={}", message.sagaId(), message.action());
        processPaymentCommandUseCase.handle(new PaymentCommand(
                message.sagaId(),
                SagaAction.valueOf(message.action()),
                message.customerName(),
                message.destination(),
                message.amount()
        ));
    }
}
