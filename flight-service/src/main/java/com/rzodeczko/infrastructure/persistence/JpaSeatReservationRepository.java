package com.rzodeczko.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaSeatReservationRepository extends JpaRepository<SeatReservationEntity, UUID> {
    boolean existsBySagaId(UUID sagaId);
    Optional<SeatReservationEntity> findBySagaId(UUID sagaId);
}
