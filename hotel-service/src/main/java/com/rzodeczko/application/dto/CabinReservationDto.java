package com.rzodeczko.application.dto;

import com.rzodeczko.domain.model.CabinReservation;

import java.time.Instant;

public record CabinReservationDto(
        String id,
        String sagaId,
        String customerName,
        String destination,
        String status,
        Instant createdAt
) {
    public static CabinReservationDto from(CabinReservation reservation) {
        return new CabinReservationDto(
                reservation.getId().toString(),
                reservation.getSagaId().toString(),
                reservation.getCustomerName(),
                reservation.getDestination(),
                reservation.getStatus().name(),
                reservation.getCreatedAt()
        );
    }
}
