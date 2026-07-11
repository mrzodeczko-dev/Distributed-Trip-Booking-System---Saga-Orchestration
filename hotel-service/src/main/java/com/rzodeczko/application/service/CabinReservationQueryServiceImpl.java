package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.CabinReservationDto;
import com.rzodeczko.application.port.in.GetCabinReservationUseCase;
import com.rzodeczko.application.port.out.CabinReservationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CabinReservationQueryServiceImpl implements GetCabinReservationUseCase {

    private final CabinReservationRepository cabinReservationRepository;

    public CabinReservationQueryServiceImpl(CabinReservationRepository cabinReservationRepository) {
        this.cabinReservationRepository = cabinReservationRepository;
    }

    @Override
    public List<CabinReservationDto> listAll() {
        return cabinReservationRepository
                .findAll()
                .stream()
                .map(CabinReservationDto::from)
                .toList();
    }

    @Override
    public Optional<CabinReservationDto> getBySagaId(UUID sagaId) {
        return cabinReservationRepository.findBySagaId(sagaId).map(CabinReservationDto::from);
    }
}
