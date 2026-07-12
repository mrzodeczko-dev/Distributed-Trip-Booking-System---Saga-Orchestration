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
    private static final String CUSTOMER = "Jan Kowalski";

    @Nested
    @DisplayName("attemptReserve - success cases")
    class SuccessCases {

        @Test
        void shouldReturnSuccessForRegularDestination() {
            ReservationOutcome outcome = ReservationOutcome.attemptReserve(SAGA_ID, CUSTOMER, "Venus");

            assertThat(outcome).isInstanceOf(ReservationOutcome.Success.class);
            ReservationOutcome.Success success = (ReservationOutcome.Success) outcome;
            assertThat(success.reservation().getSagaId()).isEqualTo(SAGA_ID);
            assertThat(success.reservation().getCustomerName()).isEqualTo(CUSTOMER);
            assertThat(success.reservation().getDestination()).isEqualTo("Venus");
            assertThat(success.reservation().getStatus()).isEqualTo(ReservationStatus.RESERVED);
        }

        @Test
        void shouldReturnSuccessWhenDestinationIsNull() {
            ReservationOutcome outcome = ReservationOutcome.attemptReserve(SAGA_ID, CUSTOMER, null);

            assertThat(outcome).isInstanceOf(ReservationOutcome.Success.class);
        }

        @Test
        void shouldReturnSuccessWhenDestinationContainsMarsAsSubstringOnly() {
            ReservationOutcome outcome = ReservationOutcome.attemptReserve(SAGA_ID, CUSTOMER, "Marseille");

            assertThat(outcome).isInstanceOf(ReservationOutcome.Success.class);
        }
    }

    @Nested
    @DisplayName("attemptReserve - rejection cases")
    class RejectionCases {

        @ParameterizedTest
        @ValueSource(strings = {"MARS", "mars", "Mars", "mArS"})
        void shouldRejectWhenDestinationIsMarsCaseInsensitive(String destination) {
            ReservationOutcome outcome = ReservationOutcome.attemptReserve(SAGA_ID, CUSTOMER, destination);

            assertThat(outcome).isInstanceOf(ReservationOutcome.Rejected.class);
            ReservationOutcome.Rejected rejected = (ReservationOutcome.Rejected) outcome;
            assertThat(rejected.reason()).isNotBlank();
        }
    }
}
