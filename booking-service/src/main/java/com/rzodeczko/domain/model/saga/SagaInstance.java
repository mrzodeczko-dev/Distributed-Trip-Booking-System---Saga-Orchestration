package com.rzodeczko.domain.model.saga;

import com.rzodeczko.domain.exception.InvalidSagaStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class SagaInstance {
    private final UUID id;
    private final String customerName;
    private final String destination;
    private final BigDecimal amount;
    private SagaStatus status;
    private final List<SagaStep> steps;
    private final Instant createdAt;
    private Instant updatedAt;

    private SagaInstance(
            UUID id,
            String customerName,
            String destination,
            BigDecimal amount,
            SagaStatus status,
            List<SagaStep> steps,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.customerName = customerName;
        this.destination = destination;
        this.amount = amount;
        this.status = status;
        this.steps = steps;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SagaInstance start(String customerName, String destination, BigDecimal amount) {
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("customerName is required");
        }

        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("destination is required");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        List<SagaStep> steps = new ArrayList<>();
        for (SagaStepName name : SagaStepName.values()) {
            steps.add(new SagaStep(name));
        }

        Instant now = Instant.now();

        return new SagaInstance(
                UUID.randomUUID(),
                customerName,
                destination,
                amount,
                SagaStatus.IN_PROGRESS,
                steps,
                now,
                now
        );
    }

    public static SagaInstance restore(
            UUID id,
            String customerName,
            String destination,
            BigDecimal amount,
            SagaStatus status,
            List<SagaStep> steps,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new SagaInstance(id, customerName, destination, amount, status, steps, createdAt, updatedAt);
    }

    public Optional<SagaStepName> nextStepToReserve() {
        return steps
                .stream()
                .filter(SagaStep::isPending)
                .map(SagaStep::getName)
                .findFirst();
    }

    public Optional<SagaStepName> nextStepToCompensate() {
        for (int i = steps.size() - 1; i >= 0; --i) {
            if (steps.get(i).isReserved()) {
                return Optional.of(steps.get(i).getName());
            }
        }
        return Optional.empty();
    }

    public SagaStep getStep(SagaStepName name) {
        return steps
                .stream()
                .filter(step -> step.getName() == name)
                .findFirst()
                .orElseThrow(() -> new InvalidSagaStateException("Unknown saga step: " + name));
    }

    public boolean isForwardPhase() {
        return status == SagaStatus.IN_PROGRESS;
    }

    public boolean isCompensating() {
        return status == SagaStatus.COMPENSATING;
    }

    public void markReserved(SagaStepName name) {
        getStep(name).changeStatus(SagaStepStatus.RESERVED, null);
        touch();
    }

    public void complete() {
        this.status = SagaStatus.COMPLETED;
        touch();
    }

    public void failAndStartCompensation(SagaStepName name, String reason) {
        getStep(name).changeStatus(SagaStepStatus.FAILED, reason);
        this.status = SagaStatus.COMPENSATING;
        touch();
    }

    public void markCompensated(SagaStepName name) {
        getStep(name).changeStatus(SagaStepStatus.COMPENSATED, null);
        touch();
    }

    public void markCompensationFailed(SagaStepName name, String reason) {
        getStep(name).changeStatus(SagaStepStatus.COMPENSATION_FAILED, reason);
        this.status = SagaStatus.COMPENSATION_FAILED;
        touch();
    }

    public void cancel() {
        this.status = SagaStatus.CANCELLED;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getDestination() {
        return destination;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public List<SagaStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
