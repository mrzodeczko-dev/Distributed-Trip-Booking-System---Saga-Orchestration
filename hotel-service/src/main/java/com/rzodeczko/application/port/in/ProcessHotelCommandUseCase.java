package com.rzodeczko.application.port.in;

import com.rzodeczko.application.command.HotelCommand;

public interface ProcessHotelCommandUseCase {
    void handle(HotelCommand command);
}
