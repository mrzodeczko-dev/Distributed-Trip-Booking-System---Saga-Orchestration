package com.rzodeczko.presentation.dto.response;

import com.rzodeczko.application.dto.SagaInstanceDto;
import com.rzodeczko.application.dto.SagaStepDto;
import com.rzodeczko.application.dto.StuckSagaDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StuckBookingResponseDtoTest {

    @Test
    void fromShouldMapSagaFieldsAndWaitingFor() {
        Instant created = Instant.parse("2026-09-27T10:00:00Z");
        Instant updated = Instant.parse("2026-09-27T10:05:00Z");
        SagaInstanceDto saga = new SagaInstanceDto(
                "saga-1", "Jan", "Mars", new BigDecimal("100"), "COMPENSATING",
                List.of(new SagaStepDto("FLIGHT", "RESERVED", null), new SagaStepDto("HOTEL", "FAILED", "No rooms")),
                created, updated);

        StuckBookingResponseDto response = StuckBookingResponseDto.from(new StuckSagaDto(saga, "CANCEL", "FLIGHT"));

        assertThat(response.sagaId()).isEqualTo("saga-1");
        assertThat(response.customerName()).isEqualTo("Jan");
        assertThat(response.destination()).isEqualTo("Mars");
        assertThat(response.amount()).isEqualByComparingTo("100");
        assertThat(response.status()).isEqualTo("COMPENSATING");
        assertThat(response.waitingFor()).isEqualTo("CANCEL FLIGHT");
        assertThat(response.steps()).extracting(SagaStepResponseDto::name).containsExactly("FLIGHT", "HOTEL");
        assertThat(response.createdAt()).isEqualTo(created);
        assertThat(response.updatedAt()).isEqualTo(updated);
    }

    @Test
    void waitingForShouldBeNullWhenNothingIsAwaited() {
        SagaInstanceDto saga = new SagaInstanceDto(
                "saga-2", "Jan", "Mars", BigDecimal.ONE, "CANCELLED", List.of(), Instant.EPOCH, Instant.EPOCH);

        assertThat(StuckBookingResponseDto.from(new StuckSagaDto(saga, null, null)).waitingFor()).isNull();
    }
}
