package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.SeatReservationDto;
import com.rzodeczko.application.port.in.GetSeatReservationUseCase;
import com.rzodeczko.application.port.out.SeatReservationRepository;
import com.rzodeczko.domain.model.SeatReservation;

import java.util.Optional;
import java.util.UUID;

public class SeatReservationQueryServiceImpl implements GetSeatReservationUseCase {

    private final SeatReservationRepository seatReservationRepository;

    public SeatReservationQueryServiceImpl(SeatReservationRepository seatReservationRepository) {
        this.seatReservationRepository = seatReservationRepository;
    }

    @Override
    public PageResult<SeatReservationDto> list(PageQuery query) {
        PageResult<SeatReservation> page = seatReservationRepository.findAll(query);
        return new PageResult<>(
                page.content().stream().map(SeatReservationDto::from).toList(),
                page.page(),
                page.size(),
                page.totalElements()
        );
    }

    @Override
    public Optional<SeatReservationDto> getBySagaId(UUID sagaId) {
        return seatReservationRepository
                .findBySagaId(sagaId)
                .map(SeatReservationDto::from);
    }
}
