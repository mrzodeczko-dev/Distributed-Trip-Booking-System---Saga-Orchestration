package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.SeatReservationDto;
import com.rzodeczko.application.port.out.SeatReservationRepository;
import com.rzodeczko.domain.model.SeatReservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatReservationQueryServiceImplTest {

    @Mock
    private SeatReservationRepository seatReservationRepository;

    private SeatReservationQueryServiceImpl service;

    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final String CUSTOMER = "Jan Kowalski";
    private static final String DESTINATION = "Mars";

    @BeforeEach
    void setUp() {
        service = new SeatReservationQueryServiceImpl(seatReservationRepository);
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        void shouldReturnAllReservationsMappedToDto() {
            SeatReservation reservation = SeatReservation.reserve(SAGA_ID, CUSTOMER, DESTINATION);
            when(seatReservationRepository.findAll()).thenReturn(List.of(reservation));

            List<SeatReservationDto> result = service.listAll();

            assertThat(result).hasSize(1);
            SeatReservationDto dto = result.get(0);
            assertThat(dto.sagaId()).isEqualTo(SAGA_ID.toString());
            assertThat(dto.customerName()).isEqualTo(CUSTOMER);
            assertThat(dto.destination()).isEqualTo(DESTINATION);
        }

        @Test
        void shouldReturnEmptyListWhenNoReservations() {
            when(seatReservationRepository.findAll()).thenReturn(List.of());

            List<SeatReservationDto> result = service.listAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBySagaId")
    class GetBySagaId {

        @Test
        void shouldReturnReservationWhenFound() {
            SeatReservation reservation = SeatReservation.reserve(SAGA_ID, CUSTOMER, DESTINATION);
            when(seatReservationRepository.findBySagaId(SAGA_ID)).thenReturn(Optional.of(reservation));

            Optional<SeatReservationDto> result = service.getBySagaId(SAGA_ID);

            assertThat(result).isPresent();
            assertThat(result.get().sagaId()).isEqualTo(SAGA_ID.toString());
            verify(seatReservationRepository).findBySagaId(SAGA_ID);
        }

        @Test
        void shouldReturnEmptyWhenNotFound() {
            when(seatReservationRepository.findBySagaId(SAGA_ID)).thenReturn(Optional.empty());

            Optional<SeatReservationDto> result = service.getBySagaId(SAGA_ID);

            assertThat(result).isEmpty();
        }
    }
}
