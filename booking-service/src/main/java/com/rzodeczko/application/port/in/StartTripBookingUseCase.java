package com.rzodeczko.application.port.in;


import com.rzodeczko.application.command.StartTripBookingCommand;
import com.rzodeczko.application.dto.SagaInstanceDto;

public interface StartTripBookingUseCase {
    SagaInstanceDto start(StartTripBookingCommand command);
}
