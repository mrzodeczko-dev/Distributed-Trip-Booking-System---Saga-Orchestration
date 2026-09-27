package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.SagaInstanceDto;
import com.rzodeczko.application.dto.StuckSagaDto;
import com.rzodeczko.application.dto.StuckSagaQuery;
import com.rzodeczko.application.port.in.GetSagaUseCase;
import com.rzodeczko.application.port.out.SagaInstanceRepository;
import com.rzodeczko.domain.exception.SagaNotFoundException;
import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SagaQueryServiceImpl implements GetSagaUseCase {

    /**
     * Statuses in which a saga still waits for a participant reply.
     */
    static final Set<SagaStatus> ACTIVE_STATUSES = EnumSet.of(SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING);

    private final SagaInstanceRepository sagaInstanceRepository;
    private final Clock clock;

    public SagaQueryServiceImpl(SagaInstanceRepository sagaInstanceRepository) {
        this(sagaInstanceRepository, Clock.systemUTC());
    }

    public SagaQueryServiceImpl(SagaInstanceRepository sagaInstanceRepository, Clock clock) {
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.clock = clock;
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
        PageResult<SagaInstance> page = sagaInstanceRepository.findAll(query);
        return new PageResult<>(
                page.content().stream().map(SagaInstanceDto::from).toList(),
                page.page(),
                page.size(),
                page.totalElements()
        );
    }

    @Override
    public List<StuckSagaDto> findStuck(StuckSagaQuery query) {
        Instant updatedBefore = clock.instant().minus(query.idleFor());
        return sagaInstanceRepository
                .findStuck(ACTIVE_STATUSES, updatedBefore, query.limit())
                .stream()
                .map(StuckSagaDto::from)
                .toList();
    }
}
