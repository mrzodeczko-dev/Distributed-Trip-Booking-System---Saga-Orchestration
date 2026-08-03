package com.rzodeczko.infrastructure.persistence.adapter;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.port.out.CabinReservationRepository;
import com.rzodeczko.domain.model.CabinReservation;
import com.rzodeczko.infrastructure.persistence.entity.CabinReservationEntity;
import com.rzodeczko.infrastructure.persistence.mapper.CabinReservationMapper;
import com.rzodeczko.infrastructure.persistence.repository.JpaCabinReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CabinReservationRepositoryAdapter implements CabinReservationRepository {

    private final JpaCabinReservationRepository repository;
    private final CabinReservationMapper mapper;

    @Override
    public void save(CabinReservation reservation) {
        repository.save(
                repository.findById(reservation.getId())
                        .map(existing -> {
                            existing.setStatus(reservation.getStatus());
                            return existing;
                        })
                        .orElseGet(() -> mapper.toEntity(reservation))
        );
    }

    @Override
    public boolean existsBySagaId(UUID sagaId) {
        return repository.existsBySagaId(sagaId);
    }

    @Override
    public Optional<CabinReservation> findBySagaId(UUID sagaId) {
        return repository.findBySagaId(sagaId).map(mapper::toDomain);
    }

    @Override
    public PageResult<CabinReservation> findAll(PageQuery query) {
        Page<CabinReservationEntity> page = repository.findAll(
                PageRequest.of(query.page(), query.size(), Sort.by("createdAt", "id").descending()));
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
