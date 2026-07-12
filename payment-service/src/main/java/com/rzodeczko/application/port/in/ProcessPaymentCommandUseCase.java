package com.rzodeczko.application.port.in;

import com.rzodeczko.application.command.PaymentCommand;

public interface ProcessPaymentCommandUseCase {
    void handle(PaymentCommand command);
}
