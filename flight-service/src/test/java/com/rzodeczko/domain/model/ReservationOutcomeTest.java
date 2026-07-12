package com.rzodeczko.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationOutcomeTest {

    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final String DESTINATION = "Europa";

    @Nested
    @DisplayName("attemptReserve()")
    class AttemptReserve {

        @Test
        void shouldSucceedForRegularCustomer() {
            ReservationOutcome outcome = ReservationOutcome.attemptReserve(
                    SAGA_ID, "Anna Nowak", DESTINATION
            );

            assertThat(outcome).isInstanceOf(ReservationOutcome.Success.class);

            var success = (ReservationOutcome.Success) outcome;
            SeatReservation reservation = success.reservation();
            assertThat(reservation.getSagaId()).isEqualTo(SAGA_ID);
            assertThat(reservation.getCustomerName()).isEqualTo("Anna Nowak");
            assertThat(reservation.getDestination()).isEqualTo(DESTINATION);
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
            assertThat(reservation.getId()).isNotNull();
            assertThat(reservation.getCreatedAt()).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"BLOCKED_USER", "blocked_test", "Blocked Customer"})
        void shouldRejectBlockedCustomer(String blockedName) {
            ReservationOutcome outcome = ReservationOutcome.attemptReserve(
                    SAGA_ID, blockedName, DESTINATION
            );

            assertThat(outcome).isInstanceOf(ReservationOutcome.Rejected.class);

            var rejected = (ReservationOutcome.Rejected) outcome;
            assertThat(rejected.reason()).isNotBlank();
        }

        @Test
        void shouldSucceedForNullCustomerName() {
            // null customerName nie zaczyna sie od BLOCKED — przechodzi walidacje domenowa
            ReservationOutcome outcome = ReservationOutcome.attemptReserve(
                    SAGA_ID, null, DESTINATION
            );

            assertThat(outcome).isInstanceOf(ReservationOutcome.Success.class);
        }
    }

    @Nested
    @DisplayName("Sealed interface exhaustiveness")
    class SealedInterface {

        @Test
        void shouldCoverAllCasesWithPatternMatching() {
            ReservationOutcome success = ReservationOutcome.attemptReserve(SAGA_ID, "OK", DESTINATION);
            ReservationOutcome rejected = ReservationOutcome.attemptReserve(SAGA_ID, "BLOCKED", DESTINATION);

            String successResult = switch (success) {
                case ReservationOutcome.Success s -> "ok:" + s.reservation().getId();
                case ReservationOutcome.Rejected r -> "fail:" + r.reason();
            };
            assertThat(successResult).startsWith("ok:");

            String rejectedResult = switch (rejected) {
                case ReservationOutcome.Success s -> "ok:" + s.reservation().getId();
                case ReservationOutcome.Rejected r -> "fail:" + r.reason();
            };
            assertThat(rejectedResult).startsWith("fail:");
        }
    }
}
