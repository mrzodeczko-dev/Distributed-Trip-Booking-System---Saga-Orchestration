package com.rzodeczko.infrastructure.tx;

import com.rzodeczko.application.command.FlightCommand;
import com.rzodeczko.application.port.in.ProcessFlightCommandUseCase;
import com.rzodeczko.application.service.FlightCommandService;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalFlightCommandService implements ProcessFlightCommandUseCase {

    private final FlightCommandService delegate;

    public TransactionalFlightCommandService(FlightCommandService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void handle(FlightCommand command) {
        delegate.handle(command);
    }
}
