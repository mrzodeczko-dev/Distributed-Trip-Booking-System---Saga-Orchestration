package com.rzodeczko.infrastructure.persistence;

import com.rzodeczko.application.port.out.SeatReservationRepository;
import com.rzodeczko.domain.model.SeatReservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
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
    public List<SeatReservation> findAll() {
        return repository
                .findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
