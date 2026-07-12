package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.command.StartTripBookingCommand;
import com.rzodeczko.application.dto.SagaInstanceDto;
import com.rzodeczko.application.dto.SagaStepDto;
import com.rzodeczko.application.port.in.GetSagaUseCase;
import com.rzodeczko.application.port.in.StartTripBookingUseCase;
import com.rzodeczko.presentation.dto.request.StartTripBookingRequestDto;
import com.rzodeczko.presentation.dto.response.BookingResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private StartTripBookingUseCase startTripBookingUseCase;
    @Mock
    private GetSagaUseCase getSagaUseCase;

    private BookingController controller;

    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        controller = new BookingController(startTripBookingUseCase, getSagaUseCase);
    }

    private SagaInstanceDto sampleDto() {
        return new SagaInstanceDto(
                SAGA_ID.toString(), "Jan", "Mars", BigDecimal.TEN,
                "IN_PROGRESS", List.of(new SagaStepDto("FLIGHT", "PENDING", null)),
                NOW, NOW
        );
    }

    @Nested
    @DisplayName("POST /bookings")
    class StartBooking {

        @Test
        @DisplayName("should return 201 with location header")
        void shouldReturn201() {
            SagaInstanceDto dto = sampleDto();
            when(startTripBookingUseCase.start(any())).thenReturn(dto);

            StartTripBookingRequestDto request = new StartTripBookingRequestDto(
                    "Jan", "Mars", BigDecimal.TEN);

            ResponseEntity<BookingResponseDto> response = controller.startBooking(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getHeaders().getLocation().toString())
                    .isEqualTo("/bookings/" + SAGA_ID);
            assertThat(response.getBody().sagaId()).isEqualTo(SAGA_ID.toString());
        }

        @Test
        @DisplayName("should pass correct command to use case")
        void shouldPassCorrectCommand() {
            when(startTripBookingUseCase.start(any())).thenReturn(sampleDto());

            controller.startBooking(new StartTripBookingRequestDto(
                    "Anna", "Europa", new BigDecimal("500")));

            ArgumentCaptor<StartTripBookingCommand> captor =
                    ArgumentCaptor.forClass(StartTripBookingCommand.class);
            verify(startTripBookingUseCase).start(captor.capture());

            StartTripBookingCommand cmd = captor.getValue();
            assertThat(cmd.customerName()).isEqualTo("Anna");
            assertThat(cmd.destination()).isEqualTo("Europa");
            assertThat(cmd.amount()).isEqualByComparingTo(new BigDecimal("500"));
        }
    }

    @Nested
    @DisplayName("GET /bookings/{sagaId}")
    class GetBooking {

        @Test
        @DisplayName("should return 200 with booking")
        void shouldReturn200() {
            SagaInstanceDto dto = sampleDto();
            when(getSagaUseCase.getById(SAGA_ID)).thenReturn(dto);

            ResponseEntity<BookingResponseDto> response = controller.getBooking(SAGA_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().customerName()).isEqualTo("Jan");
        }
    }

    @Nested
    @DisplayName("GET /bookings")
    class GetBookings {

        @Test
        @DisplayName("should return list of bookings")
        void shouldReturnList() {
            when(getSagaUseCase.listAll()).thenReturn(List.of(sampleDto()));

            ResponseEntity<List<BookingResponseDto>> response = controller.getBookings();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list")
        void shouldReturnEmptyList() {
            when(getSagaUseCase.listAll()).thenReturn(List.of());

            ResponseEntity<List<BookingResponseDto>> response = controller.getBookings();

            assertThat(response.getBody()).isEmpty();
        }
    }
}
