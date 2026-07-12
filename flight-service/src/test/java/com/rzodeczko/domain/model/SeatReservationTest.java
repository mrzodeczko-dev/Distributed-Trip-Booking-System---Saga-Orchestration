package com.rzodeczko.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SeatReservationTest {

    @Test
    @DisplayName("reserve() should create reservation with RESERVED status")
    void reserveShouldCreateWithReservedStatus() {
        UUID sagaId = UUID.randomUUID();

        SeatReservation reservation = SeatReservation.reserve(sagaId, "Jan", "Mars");

        assertThat(reservation.getId()).isNotNull();
        assertThat(reservation.getSagaId()).isEqualTo(sagaId);
        assertThat(reservation.getCustomerName()).isEqualTo("Jan");
        assertThat(reservation.getDestination()).isEqualTo("Mars");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("cancel() should change status to CANCELLED")
    void cancelShouldChangeStatus() {
        SeatReservation reservation = SeatReservation.reserve(UUID.randomUUID(), "Jan", "Mars");

        reservation.cancel();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("restore() should recreate reservation from persisted state")
    void restoreShouldRecreateState() {
        UUID id = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(3600);

        SeatReservation reservation = SeatReservation.restore(
                id, sagaId, "Anna", "Venus", ReservationStatus.CANCELLED, createdAt
        );

        assertThat(reservation.getId()).isEqualTo(id);
        assertThat(reservation.getSagaId()).isEqualTo(sagaId);
        assertThat(reservation.getCustomerName()).isEqualTo("Anna");
        assertThat(reservation.getDestination()).isEqualTo("Venus");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getCreatedAt()).isEqualTo(createdAt);
    }
}
