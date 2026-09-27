package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.dto.SagaInstanceDto;
import com.rzodeczko.application.dto.SagaStepDto;
import com.rzodeczko.application.dto.StuckSagaDto;
import com.rzodeczko.application.dto.StuckSagaQuery;
import com.rzodeczko.application.port.in.GetSagaUseCase;
import com.rzodeczko.application.port.in.StartTripBookingUseCase;
import com.rzodeczko.presentation.dto.response.StuckBookingResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingControllerStuckTest {

    @Mock
    private StartTripBookingUseCase startTripBookingUseCase;
    @Mock
    private GetSagaUseCase getSagaUseCase;

    private BookingController controller;

    @BeforeEach
    void setUp() {
        controller = new BookingController(startTripBookingUseCase, getSagaUseCase);
    }

    @Test
    void shouldReturnStuckBookingsWithAwaitedReplyAndPassQueryParameters() {
        Instant old = Instant.parse("2026-09-27T10:00:00Z");
        SagaInstanceDto saga = new SagaInstanceDto(
                UUID.randomUUID().toString(), "Jan", "Mars", BigDecimal.TEN, "IN_PROGRESS",
                List.of(new SagaStepDto("FLIGHT", "RESERVED", null), new SagaStepDto("HOTEL", "PENDING", null)),
                old, old);
        when(getSagaUseCase.findStuck(any(StuckSagaQuery.class)))
                .thenReturn(List.of(new StuckSagaDto(saga, "RESERVE", "HOTEL")));

        ResponseEntity<List<StuckBookingResponseDto>> response = controller.getStuckBookings(30, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).singleElement().satisfies(b -> {
            assertThat(b.sagaId()).isEqualTo(saga.sagaId());
            assertThat(b.waitingFor()).isEqualTo("RESERVE HOTEL");
        });
        ArgumentCaptor<StuckSagaQuery> captor = ArgumentCaptor.forClass(StuckSagaQuery.class);
        verify(getSagaUseCase).findStuck(captor.capture());
        assertThat(captor.getValue().idleFor()).isEqualTo(Duration.ofMinutes(30));
        assertThat(captor.getValue().limit()).isEqualTo(5);
    }

    @Test
    void shouldReturnEmptyListWhenNothingIsStuck() {
        when(getSagaUseCase.findStuck(any(StuckSagaQuery.class))).thenReturn(List.of());

        assertThat(controller.getStuckBookings(10, 50).getBody()).isEmpty();
    }

    @Test
    void shouldRejectInvalidParameters() {
        // IllegalArgumentException is mapped to 400 by GlobalExceptionHandler
        assertThatIllegalArgumentException().isThrownBy(() -> controller.getStuckBookings(0, 50));
        assertThatIllegalArgumentException().isThrownBy(() -> controller.getStuckBookings(10, 0));
    }
}
