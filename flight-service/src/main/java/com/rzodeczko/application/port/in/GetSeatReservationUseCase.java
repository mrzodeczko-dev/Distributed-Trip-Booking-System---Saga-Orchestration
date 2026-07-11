package com.rzodeczko.application.port.in;

import com.rzodeczko.application.dto.SeatReservationDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetSeatReservationUseCase {
    List<SeatReservationDto> listAll();
    Optional<SeatReservationDto> getBySagaId(UUID sagaId);
}
