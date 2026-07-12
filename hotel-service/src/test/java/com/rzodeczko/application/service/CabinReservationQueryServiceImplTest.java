package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.CabinReservationDto;
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
    @DisplayName("listAll")
    class ListAll {

        @Test
        void shouldMapAllReservationsToDtos() {
            CabinReservation r1 = CabinReservation.reserve(SAGA_ID, "Jan", "Venus");
            CabinReservation r2 = CabinReservation.reserve(UUID.randomUUID(), "Anna", "Earth");
            when(cabinReservationRepository.findAll()).thenReturn(List.of(r1, r2));

            List<CabinReservationDto> result = service.listAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(CabinReservationDto::customerName)
                    .containsExactlyInAnyOrder("Jan", "Anna");
        }

        @Test
        void shouldReturnEmptyListWhenNoReservations() {
            when(cabinReservationRepository.findAll()).thenReturn(List.of());

            List<CabinReservationDto> result = service.listAll();

            assertThat(result).isEmpty();
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
