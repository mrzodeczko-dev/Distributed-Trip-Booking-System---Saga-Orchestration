package com.rzodeczko.application.port.out;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.domain.model.CabinReservation;

import java.util.Optional;
import java.util.UUID;

public interface CabinReservationRepository {
    void save(CabinReservation reservation);
    boolean existsBySagaId(UUID sagaId);
    Optional<CabinReservation> findBySagaId(UUID sagaId);
    PageResult<CabinReservation> findAll(PageQuery query);
}
