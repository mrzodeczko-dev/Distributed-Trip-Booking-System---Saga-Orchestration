package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.CabinReservationDto;
import com.rzodeczko.application.port.in.GetCabinReservationUseCase;
import com.rzodeczko.presentation.dto.response.CabinReservationResponseDto;
import com.rzodeczko.presentation.dto.response.PagedResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<PagedResponseDto<CabinReservationResponseDto>> getReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<CabinReservationDto> result = getCabinReservationUseCase.list(new PageQuery(page, size));
        return ResponseEntity.ok(new PagedResponseDto<>(
                result.content().stream().map(CabinReservationResponseDto::from).toList(),
                result.page(),
                result.size(),
                result.totalElements()
        ));
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
