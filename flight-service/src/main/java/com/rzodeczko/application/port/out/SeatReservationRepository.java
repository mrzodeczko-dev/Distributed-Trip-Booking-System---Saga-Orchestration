package com.rzodeczko.application.port.out;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.domain.model.SeatReservation;

import java.util.Optional;
import java.util.UUID;

public interface SeatReservationRepository {
    void save(SeatReservation reservation);
    boolean existsBySagaId(UUID sagaId);
    Optional<SeatReservation> findBySagaId(UUID sagaId);
    PageResult<SeatReservation> findAll(PageQuery query);
}
