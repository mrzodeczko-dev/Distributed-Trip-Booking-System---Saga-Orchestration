package com.rzodeczko.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Payment {

    private final UUID id;
    private final UUID sagaId;
    private final String customerName;
    private final BigDecimal amount;
    private PaymentStatus status;
    private final Instant createdAt;

    private Payment(
            UUID id,
            UUID sagaId,
            String customerName,
            BigDecimal amount,
            PaymentStatus status,
            Instant createdAt) {
        this.id = id;
        this.sagaId = sagaId;
        this.customerName = customerName;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Payment charge(UUID sagaId, String customerName, BigDecimal amount) {
        return new Payment(
                UUID.randomUUID(),
                sagaId,
                customerName,
                amount,
                PaymentStatus.CHARGED,
                Instant.now()
        );
    }

    public static Payment restore(
            UUID id,
            UUID sagaId,
            String customerName,
            BigDecimal amount,
            PaymentStatus status,
            Instant createdAt) {
        return new Payment(id, sagaId, customerName, amount, status, createdAt);
    }

    public void refund() {
        this.status = PaymentStatus.REFUNDED;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
