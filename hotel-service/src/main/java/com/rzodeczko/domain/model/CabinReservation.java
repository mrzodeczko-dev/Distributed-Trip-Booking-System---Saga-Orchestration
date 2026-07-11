package com.rzodeczko.domain.model;

import java.time.Instant;
import java.util.UUID;

public class CabinReservation {
    private final UUID id;
    private final UUID sagaId;
    private final String customerName;
    private final String destination;
    private ReservationStatus status;
    private final Instant createdAt;

    private CabinReservation(
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

    public static CabinReservation reserve(
            UUID sagaId,
            String customerName,
            String destination
    ) {
        return new CabinReservation(
                UUID.randomUUID(),
                sagaId,
                customerName,
                destination,
                ReservationStatus.RESERVED,
                Instant.now()
        );
    }

    public static CabinReservation restore(
            UUID id,
            UUID sagaId,
            String customerName,
            String destination,
            ReservationStatus status,
            Instant createdAt) {
        return new CabinReservation(id, sagaId, customerName, destination, status, createdAt);
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
