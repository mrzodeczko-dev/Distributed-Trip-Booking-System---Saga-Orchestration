package com.rzodeczko.infrastructure.tx;

import com.rzodeczko.application.dto.SeatReservationDto;
import com.rzodeczko.application.port.in.GetSeatReservationUseCase;
import com.rzodeczko.application.service.SeatReservationQueryServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TransactionalSeatReservationQueryService implements GetSeatReservationUseCase {

    private final SeatReservationQueryServiceImpl delegate;

    public TransactionalSeatReservationQueryService(SeatReservationQueryServiceImpl delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatReservationDto> listAll() {
        return delegate.listAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SeatReservationDto> getBySagaId(UUID sagaId) {
        return delegate.getBySagaId(sagaId);
    }
}
