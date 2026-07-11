package com.rzodeczko.application.dto;

import com.rzodeczko.domain.model.SeatReservation;

import java.time.Instant;

public record SeatReservationDto(
        String id,
        String sagaId,
        String customerName,
        String destination,
        String status,
        Instant createdAt
) {
    public static SeatReservationDto from(SeatReservation reservation) {
        return new SeatReservationDto(
                reservation.getId().toString(),
                reservation.getSagaId().toString(),
                reservation.getCustomerName(),
                reservation.getDestination(),
                reservation.getStatus().name(),
                reservation.getCreatedAt()
        );
    }
}
