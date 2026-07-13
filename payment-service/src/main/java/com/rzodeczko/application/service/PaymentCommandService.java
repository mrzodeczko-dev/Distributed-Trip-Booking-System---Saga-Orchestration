package com.rzodeczko.application.service;

import com.rzodeczko.application.command.PaymentCommand;
import com.rzodeczko.application.event.CommandResult;
import com.rzodeczko.application.event.SagaParticipantReply;
import com.rzodeczko.application.port.in.ProcessPaymentCommandUseCase;
import com.rzodeczko.application.port.out.PaymentRepository;
import com.rzodeczko.application.port.out.ProcessedMessageStore;
import com.rzodeczko.application.port.out.SagaReplyPort;
import com.rzodeczko.domain.model.ChargeOutcome;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentCommandService implements ProcessPaymentCommandUseCase {

    private static final String STEP = "PAYMENT";
    private static final Logger log = LoggerFactory.getLogger(PaymentCommandService.class);

    private final ProcessedMessageStore processedMessageStore;
    private final PaymentRepository paymentRepository;
    private final SagaReplyPort sagaReplyPort;

    public PaymentCommandService(
            ProcessedMessageStore processedMessageStore,
            PaymentRepository paymentRepository,
            SagaReplyPort sagaReplyPort
    ) {
        this.processedMessageStore = processedMessageStore;
        this.paymentRepository = paymentRepository;
        this.sagaReplyPort = sagaReplyPort;
    }

    @Override
    public void handle(PaymentCommand command) {
        String messageKey = command.sagaId() + ":" + command.action();

        if (processedMessageStore.existsByMessageKey(messageKey)) {
            log.info("[PAYMENT] Duplicate command {} - already processed, skipping", messageKey);
            return;
        }

        CommandResult result = switch (command.action()) {
            case RESERVE -> charge(command);
            case CANCEL -> refund(command);
        };

        processedMessageStore.markProcessed(messageKey);
        sagaReplyPort.publish(SagaParticipantReply.from(command.sagaId(), STEP, command.action(), result));

        log.info("[PAYMENT] action={}, result={}", command.action(), result.statusString());
    }

    private CommandResult charge(PaymentCommand command) {
        if (paymentRepository.existsBySagaId(command.sagaId())) {
            return CommandResult.success();
        }

        return switch (ChargeOutcome.attemptCharge(
                command.sagaId(), command.customerName(), command.amount())) {
            case ChargeOutcome.Success success -> {
                paymentRepository.save(success.payment());
                yield CommandResult.success();
            }
            case ChargeOutcome.Rejected rejected -> CommandResult.failure(rejected.reason());
        };
    }

    private CommandResult refund(PaymentCommand command) {
        paymentRepository.findBySagaId(command.sagaId()).ifPresent(payment -> {
            payment.refund();
            paymentRepository.save(payment);
        });
        return CommandResult.success();
    }
}
