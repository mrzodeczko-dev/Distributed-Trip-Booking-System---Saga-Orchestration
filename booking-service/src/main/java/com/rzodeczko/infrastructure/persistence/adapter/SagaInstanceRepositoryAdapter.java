package com.rzodeczko.infrastructure.persistence.adapter;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.port.out.SagaInstanceRepository;
import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStatus;
import com.rzodeczko.infrastructure.persistence.entity.SagaInstanceEntity;
import com.rzodeczko.infrastructure.persistence.mapper.SagaInstanceMapper;
import com.rzodeczko.infrastructure.persistence.repository.JpaSagaInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class SagaInstanceRepositoryAdapter implements SagaInstanceRepository {

    private final JpaSagaInstanceRepository jpaSagaInstanceRepository;
    private final SagaInstanceMapper mapper;

    @Override
    public void save(SagaInstance saga) {
        SagaInstanceEntity entity = jpaSagaInstanceRepository.findByIdWithSteps(saga.getId())
                .map(existing -> {
                    mapper.updateEntity(existing, saga);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(saga));
        jpaSagaInstanceRepository.save(entity);
    }

    @Override
    public Optional<SagaInstance> findById(UUID sagaId) {
        return jpaSagaInstanceRepository.findByIdWithSteps(sagaId).map(mapper::toDomain);
    }

    @Override
    public Optional<SagaInstance> findByIdForUpdate(UUID sagaId) {
        return jpaSagaInstanceRepository.findByIdForUpdate(sagaId).map(mapper::toDomain);
    }

    @Override
    public PageResult<SagaInstance> findAll(PageQuery query) {

        Page<SagaInstanceEntity> page = jpaSagaInstanceRepository.findAllWithSteps(
                PageRequest.of(query.page(), query.size()));

        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    @Override
    public List<SagaInstance> findStuck(Set<SagaStatus> statuses, Instant updatedBefore, int limit) {
        // Two queries (ids first, then fetch with steps) - same approach as findAll, so the LIMIT
        // is applied in SQL and not in memory over a collection fetch.
        List<UUID> ids = jpaSagaInstanceRepository.findStuckIds(statuses, updatedBefore, PageRequest.of(0, limit));
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpaSagaInstanceRepository.findAllWithSteps(ids)
                .stream()
                .sorted(Comparator.comparing(SagaInstanceEntity::getUpdatedAt).thenComparing(SagaInstanceEntity::getId))
                .map(mapper::toDomain)
                .toList();
    }
}
