package com.rzodeczko.application.dto;

import com.rzodeczko.domain.model.CabinReservation;
import com.rzodeczko.domain.model.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CabinReservationDtoTest {

    @Test
    @DisplayName("from() maps all domain fields to DTO")
    void shouldMapFromDomain() {
        var id = UUID.randomUUID();
        var sagaId = UUID.randomUUID();
        var now = Instant.now();
        var reservation = CabinReservation.reserve(sagaId, "Jan", "Zakopane");

        var dto = CabinReservationDto.from(reservation);

        assertThat(dto.id()).isEqualTo(id.toString());
        assertThat(dto.sagaId()).isEqualTo(sagaId.toString());
        assertThat(dto.customerName()).isEqualTo("Jan");
        assertThat(dto.destination()).isEqualTo("Zakopane");
        assertThat(dto.status()).isEqualTo("RESERVED");
        assertThat(dto.createdAt()).isEqualTo(now);
    }
}
