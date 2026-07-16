package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.SagaInstanceDto;
import com.rzodeczko.application.port.in.GetSagaUseCase;
import com.rzodeczko.application.port.out.SagaInstanceRepository;
import com.rzodeczko.domain.exception.SagaNotFoundException;

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
    public PageResult<SagaInstanceDto> list(PageQuery query) {
        PageResult<com.rzodeczko.domain.model.saga.SagaInstance> page = sagaInstanceRepository.findAll(query);
        return new PageResult<>(
                page.content().stream().map(SagaInstanceDto::from).toList(),
                page.page(),
                page.size(),
                page.totalElements()
        );
    }
}
