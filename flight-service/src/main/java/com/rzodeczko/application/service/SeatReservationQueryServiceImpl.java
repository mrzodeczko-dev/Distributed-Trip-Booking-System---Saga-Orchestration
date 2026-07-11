package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.SeatReservationDto;
import com.rzodeczko.application.port.in.GetSeatReservationUseCase;
import com.rzodeczko.application.port.out.SeatReservationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SeatReservationQueryServiceImpl implements GetSeatReservationUseCase {

    private final SeatReservationRepository seatReservationRepository;

    public SeatReservationQueryServiceImpl(SeatReservationRepository seatReservationRepository) {
        this.seatReservationRepository = seatReservationRepository;
    }

    @Override
    public List<SeatReservationDto> listAll() {
        return seatReservationRepository
                .findAll()
                .stream()
                .map(SeatReservationDto::from)
                .toList();
    }

    @Override
    public Optional<SeatReservationDto> getBySagaId(UUID sagaId) {
        return seatReservationRepository
                .findBySagaId(sagaId)
                .map(SeatReservationDto::from);
    }
}
