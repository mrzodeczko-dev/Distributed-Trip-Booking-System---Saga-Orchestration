package com.rzodeczko.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CabinReservationTest {

    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final String CUSTOMER = "Jan Kowalski";
    private static final String DESTINATION = "Venus";

    @Nested
    @DisplayName("reserve")
    class Reserve {

        @Test
        void shouldCreateReservationWithGeneratedIdAndReservedStatus() {
            CabinReservation reservation = CabinReservation.reserve(SAGA_ID, CUSTOMER, DESTINATION);

            assertThat(reservation.getId()).isNotNull();
            assertThat(reservation.getSagaId()).isEqualTo(SAGA_ID);
            assertThat(reservation.getCustomerName()).isEqualTo(CUSTOMER);
            assertThat(reservation.getDestination()).isEqualTo(DESTINATION);
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
            assertThat(reservation.getCreatedAt()).isNotNull();
        }

        @Test
        void shouldGenerateDifferentIdsForEachReservation() {
            CabinReservation first = CabinReservation.reserve(SAGA_ID, CUSTOMER, DESTINATION);
            CabinReservation second = CabinReservation.reserve(SAGA_ID, CUSTOMER, DESTINATION);

            assertThat(first.getId()).isNotEqualTo(second.getId());
        }
    }

    @Nested
    @DisplayName("restore")
    class Restore {

        @Test
        void shouldRestoreReservationWithGivenFields() {
            UUID id = UUID.randomUUID();
            Instant createdAt = Instant.parse("2024-01-01T10:00:00Z");

            CabinReservation reservation = CabinReservation.restore(
                    id, SAGA_ID, CUSTOMER, DESTINATION, ReservationStatus.CANCELLED, createdAt
            );

            assertThat(reservation.getId()).isEqualTo(id);
            assertThat(reservation.getSagaId()).isEqualTo(SAGA_ID);
            assertThat(reservation.getCustomerName()).isEqualTo(CUSTOMER);
            assertThat(reservation.getDestination()).isEqualTo(DESTINATION);
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(reservation.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        void shouldChangeStatusToCancelled() {
            CabinReservation reservation = CabinReservation.reserve(SAGA_ID, CUSTOMER, DESTINATION);

            reservation.cancel();

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        void shouldBeIdempotentWhenCalledTwice() {
            CabinReservation reservation = CabinReservation.reserve(SAGA_ID, CUSTOMER, DESTINATION);

            reservation.cancel();
            reservation.cancel();

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }
    }
}
