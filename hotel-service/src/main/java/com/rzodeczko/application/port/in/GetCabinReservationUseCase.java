package com.rzodeczko.application.port.in;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.CabinReservationDto;

import java.util.Optional;
import java.util.UUID;

public interface GetCabinReservationUseCase {
    PageResult<CabinReservationDto> list(PageQuery query);
    Optional<CabinReservationDto> getBySagaId(UUID sagaId);
}
