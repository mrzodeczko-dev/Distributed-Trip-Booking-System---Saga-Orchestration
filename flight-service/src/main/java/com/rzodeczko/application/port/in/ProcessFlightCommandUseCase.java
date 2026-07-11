package com.rzodeczko.application.port.in;

import com.rzodeczko.application.command.FlightCommand;

public interface ProcessFlightCommandUseCase {
    void handle(FlightCommand command);
}
