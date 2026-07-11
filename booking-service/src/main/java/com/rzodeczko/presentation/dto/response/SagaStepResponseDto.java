package com.rzodeczko.presentation.dto.response;

import com.rzodeczko.application.dto.SagaStepDto;

public record SagaStepResponseDto(
        String name,
        String status,
        String reason
) {
    public static SagaStepResponseDto from(SagaStepDto dto) {
        return new SagaStepResponseDto(dto.name(), dto.status(), dto.reason());
    }
}
