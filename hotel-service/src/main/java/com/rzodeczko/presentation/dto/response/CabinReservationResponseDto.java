package com.rzodeczko.presentation.dto.response;

import com.rzodeczko.application.dto.CabinReservationDto;

import java.time.Instant;

public record CabinReservationResponseDto(
        String id,
        String sagaId,
        String customerName,
        String destination,
        String status,
        Instant createdAt
) {
    public static CabinReservationResponseDto from(CabinReservationDto dto) {
        return new CabinReservationResponseDto(
                dto.id(),
                dto.sagaId(),
                dto.customerName(),
                dto.destination(),
                dto.status(),
                dto.createdAt()
        );
    }
}
