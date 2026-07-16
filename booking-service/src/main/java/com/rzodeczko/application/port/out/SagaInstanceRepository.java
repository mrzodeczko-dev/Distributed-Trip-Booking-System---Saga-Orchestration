package com.rzodeczko.application.port.out;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.domain.model.saga.SagaInstance;

import java.util.Optional;
import java.util.UUID;

public interface SagaInstanceRepository {
    void save(SagaInstance saga);

    Optional<SagaInstance> findById(UUID sagaId);

    Optional<SagaInstance> findByIdForUpdate(UUID sagaId);

    PageResult<SagaInstance> findAll(PageQuery query);
}
