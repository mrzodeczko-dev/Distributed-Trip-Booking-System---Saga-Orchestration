package com.rzodeczko.infrastructure.persistence.mapper;

import com.rzodeczko.domain.model.CabinReservation;
import com.rzodeczko.infrastructure.persistence.entity.CabinReservationEntity;
import org.springframework.stereotype.Component;

@Component
public class CabinReservationMapper {
    public CabinReservationEntity toEntity(CabinReservation reservation) {
        return CabinReservationEntity
                .builder()
                .id(reservation.getId())
                .sagaId(reservation.getSagaId())
                .customerName(reservation.getCustomerName())
                .destination(reservation.getDestination())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .build();
    }

    public CabinReservation toDomain(CabinReservationEntity entity) {
        return CabinReservation.restore(
                entity.getId(),
                entity.getSagaId(),
                entity.getCustomerName(),
                entity.getDestination(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
