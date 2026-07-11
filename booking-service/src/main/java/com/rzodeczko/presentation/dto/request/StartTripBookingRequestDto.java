package com.rzodeczko.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record StartTripBookingRequestDto(
        @NotBlank String customerName,
        @NotBlank String destination,
        @NotNull @Positive BigDecimal amount
        ) {
}
