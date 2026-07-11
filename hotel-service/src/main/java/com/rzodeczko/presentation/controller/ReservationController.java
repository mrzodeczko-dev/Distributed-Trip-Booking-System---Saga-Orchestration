package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.port.in.GetCabinReservationUseCase;
import com.rzodeczko.presentation.dto.response.CabinReservationResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reservations")
public class ReservationController {
    private final GetCabinReservationUseCase getCabinReservationUseCase;

    public ReservationController(
            @Qualifier("transactionalCabinReservationQueryService")
            GetCabinReservationUseCase getCabinReservationUseCase) {
        this.getCabinReservationUseCase = getCabinReservationUseCase;
    }

    @GetMapping
    public ResponseEntity<List<CabinReservationResponseDto>> listAll() {
        return ResponseEntity.ok(getCabinReservationUseCase
                .listAll()
                .stream()
                .map(CabinReservationResponseDto::from)
                .toList()
        );
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<CabinReservationResponseDto> getBySaga(@PathVariable UUID sagaId) {
        return getCabinReservationUseCase
                .getBySagaId(sagaId)
                .map(CabinReservationResponseDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
