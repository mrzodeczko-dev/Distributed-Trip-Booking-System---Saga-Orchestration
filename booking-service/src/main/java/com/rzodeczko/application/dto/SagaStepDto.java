package com.rzodeczko.application.dto;


import com.rzodeczko.domain.model.saga.SagaStep;

public record SagaStepDto(String name, String status, String reason) {
    public static SagaStepDto from(SagaStep step) {
        return new SagaStepDto(step.getName().name(), step.getStatus().name(), step.getReason());
    }
}
