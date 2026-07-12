package com.rzodeczko.infrastructure.tx;

import com.rzodeczko.application.command.PaymentCommand;
import com.rzodeczko.application.port.in.ProcessPaymentCommandUseCase;
import com.rzodeczko.application.service.PaymentCommandService;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalPaymentCommandService implements ProcessPaymentCommandUseCase {

    private final PaymentCommandService delegate;

    public TransactionalPaymentCommandService(PaymentCommandService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void handle(PaymentCommand command) {
        delegate.handle(command);
    }
}
