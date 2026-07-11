package com.rzodeczko.application.port.in;

import com.rzodeczko.application.dto.CabinReservationDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetCabinReservationUseCase {
    List<CabinReservationDto> listAll();
    Optional<CabinReservationDto> getBySagaId(UUID sagaId);
}
