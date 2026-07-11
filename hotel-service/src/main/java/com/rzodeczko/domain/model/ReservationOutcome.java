package com.rzodeczko.domain.model;

import java.util.UUID;

public sealed interface ReservationOutcome permits ReservationOutcome.Success, ReservationOutcome.Rejected {
    record Success(CabinReservation reservation) implements ReservationOutcome {
    }

    record Rejected(String reason) implements ReservationOutcome {
    }

    static ReservationOutcome attemptReserve(UUID sagaId, String customerName, String destination) {
        if (destination != null && destination.equalsIgnoreCase("MARS")) {
            return new Rejected("No orbital cabins left for MARS - fully blocked");
        }
        return new Success(CabinReservation.reserve(sagaId, customerName, destination));
    }
}

