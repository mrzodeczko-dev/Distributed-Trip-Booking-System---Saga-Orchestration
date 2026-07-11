package com.rzodeczko.infrastructure.persistence.repository;

import com.rzodeczko.infrastructure.persistence.entity.CabinReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaCabinReservationRepository extends JpaRepository<CabinReservationEntity, UUID> {
    boolean existsBySagaId(UUID sagaId);

    Optional<CabinReservationEntity> findBySagaId(UUID sagaId);
}
