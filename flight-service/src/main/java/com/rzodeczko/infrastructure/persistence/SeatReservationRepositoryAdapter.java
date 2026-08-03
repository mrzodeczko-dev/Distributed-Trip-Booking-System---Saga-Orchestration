package com.rzodeczko.infrastructure.persistence;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.port.out.SeatReservationRepository;
import com.rzodeczko.domain.model.SeatReservation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SeatReservationRepositoryAdapter implements SeatReservationRepository {

    private final JpaSeatReservationRepository repository;
    private final SeatReservationMapper mapper;

    @Override
    public void save(SeatReservation reservation) {
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
    public Optional<SeatReservation> findBySagaId(UUID sagaId) {
        return repository.findBySagaId(sagaId).map(mapper::toDomain);
    }

    @Override
    public PageResult<SeatReservation> findAll(PageQuery query) {
        Page<SeatReservationEntity> page = repository.findAll(
                PageRequest.of(query.page(), query.size(), Sort.by("createdAt", "id").descending()));
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
