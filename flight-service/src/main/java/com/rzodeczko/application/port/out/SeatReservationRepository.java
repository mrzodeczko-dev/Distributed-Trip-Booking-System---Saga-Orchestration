package com.rzodeczko.application.port.out;

import com.rzodeczko.domain.model.SeatReservation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatReservationRepository {
    void save(SeatReservation reservation);
    boolean existsBySagaId(UUID sagaId);
    Optional<SeatReservation> findBySagaId(UUID sagaId);
    List<SeatReservation> findAll();
}
