package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.CabinReservationDto;
import com.rzodeczko.application.port.in.GetCabinReservationUseCase;
import com.rzodeczko.application.port.out.CabinReservationRepository;
import com.rzodeczko.domain.model.CabinReservation;

import java.util.Optional;
import java.util.UUID;

public class CabinReservationQueryServiceImpl implements GetCabinReservationUseCase {

    private final CabinReservationRepository cabinReservationRepository;

    public CabinReservationQueryServiceImpl(CabinReservationRepository cabinReservationRepository) {
        this.cabinReservationRepository = cabinReservationRepository;
    }

    @Override
    public PageResult<CabinReservationDto> list(PageQuery query) {
        PageResult<CabinReservation> page = cabinReservationRepository.findAll(query);
        return new PageResult<>(
                page.content().stream().map(CabinReservationDto::from).toList(),
                page.page(),
                page.size(),
                page.totalElements()
        );
    }

    @Override
    public Optional<CabinReservationDto> getBySagaId(UUID sagaId) {
        return cabinReservationRepository.findBySagaId(sagaId).map(CabinReservationDto::from);
    }
}
