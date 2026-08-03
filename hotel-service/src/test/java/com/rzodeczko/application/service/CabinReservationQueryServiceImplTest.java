package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.CabinReservationDto;
import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.port.out.CabinReservationRepository;
import com.rzodeczko.domain.model.CabinReservation;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CabinReservationQueryServiceImplTest {

    @Mock
    private CabinReservationRepository cabinReservationRepository;

    private CabinReservationQueryServiceImpl service;

    private static final UUID SAGA_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CabinReservationQueryServiceImpl(cabinReservationRepository);
    }

    @Nested
    @DisplayName("list")
    class ListPaginated {

        @Test
        void shouldMapAllReservationsToDtos() {
            CabinReservation r1 = CabinReservation.reserve(SAGA_ID, "Jan", "Venus");
            CabinReservation r2 = CabinReservation.reserve(UUID.randomUUID(), "Anna", "Earth");
            PageQuery query = new PageQuery(0, 20);
            when(cabinReservationRepository.findAll(query))
                    .thenReturn(new PageResult<>(List.of(r1, r2), 0, 20, 2));

            PageResult<CabinReservationDto> result = service.list(query);

            assertThat(result.content()).hasSize(2);
            assertThat(result.content()).extracting(CabinReservationDto::customerName)
                    .containsExactlyInAnyOrder("Jan", "Anna");
            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(20);
        }

        @Test
        void shouldReturnEmptyPageWhenNoReservations() {
            PageQuery query = new PageQuery(0, 20);
            when(cabinReservationRepository.findAll(query))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0));

            PageResult<CabinReservationDto> result = service.list(query);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("getBySagaId")
    class GetBySagaId {

        @Test
        void shouldReturnDtoWhenReservationExists() {
            CabinReservation reservation = CabinReservation.reserve(SAGA_ID, "Jan", "Venus");
            when(cabinReservationRepository.findBySagaId(SAGA_ID)).thenReturn(Optional.of(reservation));

            Optional<CabinReservationDto> result = service.getBySagaId(SAGA_ID);

            assertThat(result).isPresent();
            assertThat(result.get().sagaId()).isEqualTo(SAGA_ID.toString());
            assertThat(result.get().customerName()).isEqualTo("Jan");
        }

        @Test
        void shouldReturnEmptyWhenReservationDoesNotExist() {
            when(cabinReservationRepository.findBySagaId(SAGA_ID)).thenReturn(Optional.empty());

            Optional<CabinReservationDto> result = service.getBySagaId(SAGA_ID);

            assertThat(result).isEmpty();
        }
    }
}
