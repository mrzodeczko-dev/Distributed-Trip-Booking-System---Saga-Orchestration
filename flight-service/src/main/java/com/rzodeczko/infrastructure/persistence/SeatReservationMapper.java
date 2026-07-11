package com.rzodeczko.infrastructure.persistence;

import com.rzodeczko.domain.model.SeatReservation;
import org.springframework.stereotype.Component;

@Component
public class SeatReservationMapper {
    public SeatReservationEntity toEntity(SeatReservation reservation) {
        return SeatReservationEntity.builder()
                .id(reservation.getId())
                .sagaId(reservation.getSagaId())
                .customerName(reservation.getCustomerName())
                .destination(reservation.getDestination())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .build();
    }

    public SeatReservation toDomain(SeatReservationEntity entity) {
        return SeatReservation.restore(
                entity.getId(),
                entity.getSagaId(),
                entity.getCustomerName(),
                entity.getDestination(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
