package com.rzodeczko.infrastructure.tx;

import com.rzodeczko.application.dto.CabinReservationDto;
import com.rzodeczko.application.port.in.GetCabinReservationUseCase;
import com.rzodeczko.application.service.CabinReservationQueryServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TransactionalCabinReservationQueryService implements GetCabinReservationUseCase {

    private final CabinReservationQueryServiceImpl delegate;

    public TransactionalCabinReservationQueryService(CabinReservationQueryServiceImpl delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CabinReservationDto> listAll() {
        return delegate.listAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CabinReservationDto> getBySagaId(UUID sagaId) {
        return delegate.getBySagaId(sagaId);
    }
}
