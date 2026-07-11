package com.rzodeczko.infrastructure.tx;

import com.rzodeczko.application.command.HotelCommand;
import com.rzodeczko.application.port.in.ProcessHotelCommandUseCase;
import com.rzodeczko.application.service.HotelCommandService;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalHotelCommandService implements ProcessHotelCommandUseCase {
    private final HotelCommandService delegate;

    public TransactionalHotelCommandService(HotelCommandService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void handle(HotelCommand command) {
        delegate.handle(command);
    }
}
