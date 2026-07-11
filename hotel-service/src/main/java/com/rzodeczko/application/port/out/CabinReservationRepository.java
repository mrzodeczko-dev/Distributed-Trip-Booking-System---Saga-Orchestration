package com.rzodeczko.application.port.out;

import com.rzodeczko.domain.model.CabinReservation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CabinReservationRepository {
    void save(CabinReservation reservation);
    boolean existsBySagaId(UUID sagaId);
    Optional<CabinReservation> findBySagaId(UUID sagaId);
    List<CabinReservation> findAll();
}
