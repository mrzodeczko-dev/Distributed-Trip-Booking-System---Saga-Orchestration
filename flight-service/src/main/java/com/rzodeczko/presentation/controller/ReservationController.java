package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.SeatReservationDto;
import com.rzodeczko.application.port.in.GetSeatReservationUseCase;
import com.rzodeczko.presentation.dto.response.PagedResponseDto;
import com.rzodeczko.presentation.dto.response.SeatReservationResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reservations")
public class ReservationController {
    private final GetSeatReservationUseCase getSeatReservationUseCase;

    public ReservationController(
            @Qualifier("transactionalSeatReservationQueryService")
            GetSeatReservationUseCase getSeatReservationUseCase) {
        this.getSeatReservationUseCase = getSeatReservationUseCase;
    }

    @GetMapping
    public ResponseEntity<PagedResponseDto<SeatReservationResponseDto>> getReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<SeatReservationDto> result = getSeatReservationUseCase.list(new PageQuery(page, size));
        return ResponseEntity.ok(new PagedResponseDto<>(
                result.content().stream().map(SeatReservationResponseDto::from).toList(),
                result.page(),
                result.size(),
                result.totalElements()
        ));
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<SeatReservationResponseDto> getBySaga(@PathVariable UUID sagaId) {
        return getSeatReservationUseCase
                .getBySagaId(sagaId)
                .map(SeatReservationResponseDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
