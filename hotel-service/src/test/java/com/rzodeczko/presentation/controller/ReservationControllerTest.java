package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.dto.CabinReservationDto;
import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.port.in.GetCabinReservationUseCase;
import com.rzodeczko.presentation.dto.response.CabinReservationResponseDto;
import com.rzodeczko.presentation.dto.response.PagedResponseDto;
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
    private GetCabinReservationUseCase getCabinReservationUseCase;

    private ReservationController controller;

    private static final UUID SAGA_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new ReservationController(getCabinReservationUseCase);
    }

    @Nested
    @DisplayName("getReservations")
    class GetReservations {

        @Test
        void shouldReturnOkWithMappedReservations() {
            CabinReservationDto dto = new CabinReservationDto(
                    UUID.randomUUID().toString(), SAGA_ID.toString(), "Jan", "Venus", "RESERVED", Instant.now()
            );
            when(getCabinReservationUseCase.list(new PageQuery(0, 20)))
                    .thenReturn(new PageResult<>(List.of(dto), 0, 20, 1));

            ResponseEntity<PagedResponseDto<CabinReservationResponseDto>> response = controller.getReservations(0, 20);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).hasSize(1);
            assertThat(response.getBody().content().get(0).customerName()).isEqualTo("Jan");
            assertThat(response.getBody().totalElements()).isEqualTo(1);
            assertThat(response.getBody().page()).isZero();
            assertThat(response.getBody().size()).isEqualTo(20);
        }

        @Test
        void shouldReturnOkWithEmptyPageWhenNoReservations() {
            when(getCabinReservationUseCase.list(new PageQuery(0, 20)))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0));

            ResponseEntity<PagedResponseDto<CabinReservationResponseDto>> response = controller.getReservations(0, 20);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).isEmpty();
            assertThat(response.getBody().totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("getBySaga")
    class GetBySaga {

        @Test
        void shouldReturnOkWhenReservationFound() {
            CabinReservationDto dto = new CabinReservationDto(
                    UUID.randomUUID().toString(), SAGA_ID.toString(), "Jan", "Venus", "RESERVED", Instant.now()
            );
            when(getCabinReservationUseCase.getBySagaId(SAGA_ID)).thenReturn(Optional.of(dto));

            ResponseEntity<CabinReservationResponseDto> response = controller.getBySaga(SAGA_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().sagaId()).isEqualTo(SAGA_ID.toString());
        }

        @Test
        void shouldReturnNotFoundWhenReservationMissing() {
            when(getCabinReservationUseCase.getBySagaId(SAGA_ID)).thenReturn(Optional.empty());

            ResponseEntity<CabinReservationResponseDto> response = controller.getBySaga(SAGA_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNull();
        }
    }
}
