package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.command.StartTripBookingCommand;
import com.rzodeczko.application.port.in.GetSagaUseCase;
import com.rzodeczko.application.port.in.StartTripBookingUseCase;
import com.rzodeczko.presentation.dto.request.StartTripBookingRequestDto;
import com.rzodeczko.presentation.dto.response.BookingResponseDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final StartTripBookingUseCase startTripBookingUseCase;
    private final GetSagaUseCase getSagaUseCase;

    public BookingController(
            @Qualifier("transactionalSagaOrchestrator")
            StartTripBookingUseCase startTripBookingUseCase,
            @Qualifier("transactionalSagaQueryService")
            GetSagaUseCase getSagaUseCase) {
        this.startTripBookingUseCase = startTripBookingUseCase;
        this.getSagaUseCase = getSagaUseCase;
    }

    @PostMapping
    public ResponseEntity<BookingResponseDto> startBooking(
            @Valid @RequestBody StartTripBookingRequestDto request) {
        BookingResponseDto response = BookingResponseDto.from(startTripBookingUseCase.start(
                new StartTripBookingCommand(
                        request.customerName(),
                        request.destination(),
                        request.amount()
                )
        ));

        return ResponseEntity
                .created(URI.create("/bookings/" + response.sagaId()))
                .body(response);
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<BookingResponseDto> getBooking(@PathVariable UUID sagaId) {
        return ResponseEntity.ok(BookingResponseDto.from(getSagaUseCase.getById(sagaId)));
    }

    @GetMapping
    public ResponseEntity<List<BookingResponseDto>> getBookings() {
        return ResponseEntity.ok(getSagaUseCase
                .listAll()
                .stream()
                .map(BookingResponseDto::from)
                .toList());
    }
}
