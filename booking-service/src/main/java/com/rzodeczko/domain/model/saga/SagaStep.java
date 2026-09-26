package com.rzodeczko.domain.model.saga;

public class SagaStep {
    private final SagaStepName name;
    private SagaStepStatus status;
    private String reason;

    public SagaStep(SagaStepName name) {
        this.name = name;
        this.status = SagaStepStatus.PENDING;
    }

    private SagaStep(SagaStepName name, SagaStepStatus status, String reason) {
        this.name = name;
        this.status = status;
        this.reason = reason;
    }

    public static SagaStep restore(SagaStepName name, SagaStepStatus status, String reason) {
        return new SagaStep(name, status, reason);
    }

    void changeStatus(SagaStepStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public SagaStepName getName() {
        return name;
    }

    public SagaStepStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public boolean isReserved() {
        return status == SagaStepStatus.RESERVED;
    }

    public boolean isPending() {
        return status == SagaStepStatus.PENDING;
    }

    public boolean isCompensated() {
        return status == SagaStepStatus.COMPENSATED;
    }
}
