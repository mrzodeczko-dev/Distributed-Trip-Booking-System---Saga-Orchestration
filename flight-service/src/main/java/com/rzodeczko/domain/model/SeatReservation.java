package com.rzodeczko.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Rezerwacja miejsca w rakiecie powiazana z konkretna Saga (sagaId)
 * Lokalna transakcja uczestnika sagi
 */

public class SeatReservation {
    private final UUID id;
    private final UUID sagaId;
    private final String customerName;
    private final String destination;
    private ReservationStatus status;
    private final Instant createdAt;

    private SeatReservation(
            UUID id,
            UUID sagaId,
            String customerName,
            String destination,
            ReservationStatus status,
            Instant createdAt) {
        this.id = id;
        this.sagaId = sagaId;
        this.customerName = customerName;
        this.destination = destination;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static SeatReservation reserve(UUID sagaId, String customerName, String destination) {
        return new SeatReservation(
                UUID.randomUUID(),
                sagaId,
                customerName,
                destination,
                ReservationStatus.RESERVED,
                Instant.now()
        );
    }

    public static SeatReservation restore(
            UUID id,
            UUID sagaId,
            String customerName,
            String destination,
            ReservationStatus status,
            Instant createdAt
    ) {
        return new SeatReservation(id, sagaId, customerName, destination, status, createdAt);
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getDestination() {
        return destination;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
