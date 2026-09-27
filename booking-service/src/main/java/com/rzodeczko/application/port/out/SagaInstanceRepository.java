package com.rzodeczko.application.port.out;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SagaInstanceRepository {
    void save(SagaInstance saga);

    Optional<SagaInstance> findById(UUID sagaId);

    Optional<SagaInstance> findByIdForUpdate(UUID sagaId);

    PageResult<SagaInstance> findAll(PageQuery query);

    /**
     * Sagas in one of {@code statuses} whose last update happened before {@code updatedBefore},
     * ordered by last update (oldest first), at most {@code limit} results.
     */
    List<SagaInstance> findStuck(Set<SagaStatus> statuses, Instant updatedBefore, int limit);
}
