package com.rzodeczko.application.service;


import com.rzodeczko.application.dto.SagaInstanceDto;
import com.rzodeczko.application.port.in.GetSagaUseCase;
import com.rzodeczko.application.port.out.SagaInstanceRepository;
import com.rzodeczko.domain.exception.SagaNotFoundException;

import java.util.List;
import java.util.UUID;

public class SagaQueryServiceImpl implements GetSagaUseCase {

    private final SagaInstanceRepository sagaInstanceRepository;

    public SagaQueryServiceImpl(SagaInstanceRepository sagaInstanceRepository) {
        this.sagaInstanceRepository = sagaInstanceRepository;
    }

    @Override
    public SagaInstanceDto getById(UUID sagaId) {
        return sagaInstanceRepository
                .findById(sagaId)
                .map(SagaInstanceDto::from)
                .orElseThrow(() -> new SagaNotFoundException(sagaId));
    }

    @Override
    public List<SagaInstanceDto> listAll() {
        return sagaInstanceRepository
                .findAll()
                .stream()
                .map(SagaInstanceDto::from)
                .toList();
    }
}
