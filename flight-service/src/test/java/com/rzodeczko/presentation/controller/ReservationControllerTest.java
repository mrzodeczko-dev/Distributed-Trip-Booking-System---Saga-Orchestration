package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.dto.SeatReservationDto;
import com.rzodeczko.application.port.in.GetSeatReservationUseCase;
import com.rzodeczko.presentation.dto.response.SeatReservationResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock
    private GetSeatReservationUseCase getSeatReservationUseCase;

    private ReservationController controller;

    private static final UUID SAGA_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new ReservationController(getSeatReservationUseCase);
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        void shouldReturnOkWithMappedReservations() {
            SeatReservationDto dto = new SeatReservationDto(
                    UUID.randomUUID().toString(), SAGA_ID.toString(), "Jan", "Mars", "RESERVED", Instant.now()
            );
            when(getSeatReservationUseCase.listAll()).thenReturn(List.of(dto));

            ResponseEntity<List<SeatReservationResponseDto>> response = controller.listAll();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).sagaId()).isEqualTo(SAGA_ID.toString());
        }

        @Test
        void shouldReturnOkWithEmptyListWhenNoReservations() {
            when(getSeatReservationUseCase.listAll()).thenReturn(List.of());

            ResponseEntity<List<SeatReservationResponseDto>> response = controller.listAll();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBySaga")
    class GetBySaga {

        @Test
        void shouldReturnOkWhenFound() {
            SeatReservationDto dto = new SeatReservationDto(
                    UUID.randomUUID().toString(), SAGA_ID.toString(), "Jan", "Mars", "RESERVED", Instant.now()
            );
            when(getSeatReservationUseCase.getBySagaId(SAGA_ID)).thenReturn(Optional.of(dto));

            ResponseEntity<SeatReservationResponseDto> response = controller.getBySaga(SAGA_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().sagaId()).isEqualTo(SAGA_ID.toString());
        }

        @Test
        void shouldReturnNotFoundWhenMissing() {
            when(getSeatReservationUseCase.getBySagaId(SAGA_ID)).thenReturn(Optional.empty());

            ResponseEntity<SeatReservationResponseDto> response = controller.getBySaga(SAGA_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNull();
        }
    }
}
