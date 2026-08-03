package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
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
    @DisplayName("list")
    class ListPaged {

        private final PageQuery pageQuery = new PageQuery(0, 20);

        @Test
        void shouldReturnPagedReservationsMappedToDto() {
            SeatReservation reservation = SeatReservation.reserve(SAGA_ID, CUSTOMER, DESTINATION);
            PageResult<SeatReservation> pageResult = new PageResult<>(List.of(reservation), 0, 20, 1);
            when(seatReservationRepository.findAll(pageQuery)).thenReturn(pageResult);

            PageResult<SeatReservationDto> result = service.list(pageQuery);

            assertThat(result.content()).hasSize(1);
            assertThat(result.page()).isEqualTo(0);
            assertThat(result.size()).isEqualTo(20);
            assertThat(result.totalElements()).isEqualTo(1);
            SeatReservationDto dto = result.content().get(0);
            assertThat(dto.sagaId()).isEqualTo(SAGA_ID.toString());
            assertThat(dto.customerName()).isEqualTo(CUSTOMER);
            assertThat(dto.destination()).isEqualTo(DESTINATION);
        }

        @Test
        void shouldReturnEmptyPageWhenNoReservations() {
            PageResult<SeatReservation> pageResult = new PageResult<>(List.of(), 0, 20, 0);
            when(seatReservationRepository.findAll(pageQuery)).thenReturn(pageResult);

            PageResult<SeatReservationDto> result = service.list(pageQuery);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isEqualTo(0);
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
