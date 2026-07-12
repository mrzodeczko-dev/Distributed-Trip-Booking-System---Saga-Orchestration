package com.rzodeczko.infrastructure.persistence;

import com.rzodeczko.domain.model.ReservationStatus;
import com.rzodeczko.domain.model.SeatReservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatReservationRepositoryAdapterTest {

    @Mock
    private JpaSeatReservationRepository repository;

    @Mock
    private SeatReservationMapper mapper;

    @Captor
    private ArgumentCaptor<SeatReservationEntity> entityCaptor;

    private SeatReservationRepositoryAdapter adapter;

    private static final UUID ID = UUID.randomUUID();
    private static final UUID SAGA_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new SeatReservationRepositoryAdapter(repository, mapper);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        void shouldUpdateStatusOnExistingEntity() {
            SeatReservation domain = SeatReservation.restore(
                    ID, SAGA_ID, "Jan", "Mars", ReservationStatus.CANCELLED, Instant.now()
            );
            SeatReservationEntity existing = SeatReservationEntity.builder()
                    .id(ID)
                    .sagaId(SAGA_ID)
                    .customerName("Jan")
                    .destination("Mars")
                    .status(ReservationStatus.RESERVED)
                    .createdAt(Instant.now())
                    .build();
            when(repository.findById(ID)).thenReturn(Optional.of(existing));

            adapter.save(domain);

            verify(repository).save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            verify(mapper, never()).toEntity(domain);
        }

        @Test
        void shouldCreateNewEntityWhenNotFound() {
            SeatReservation domain = SeatReservation.restore(
                    ID, SAGA_ID, "Jan", "Mars", ReservationStatus.RESERVED, Instant.now()
            );
            SeatReservationEntity mapped = SeatReservationEntity.builder().id(ID).build();
            when(repository.findById(ID)).thenReturn(Optional.empty());
            when(mapper.toEntity(domain)).thenReturn(mapped);

            adapter.save(domain);

            verify(repository).save(mapped);
        }
    }

    @Nested
    @DisplayName("existsBySagaId")
    class ExistsBySagaId {

        @Test
        void shouldDelegateToRepository() {
            when(repository.existsBySagaId(SAGA_ID)).thenReturn(true);

            boolean result = adapter.existsBySagaId(SAGA_ID);

            assertThat(result).isTrue();
            verify(repository).existsBySagaId(SAGA_ID);
        }
    }

    @Nested
    @DisplayName("findBySagaId")
    class FindBySagaId {

        @Test
        void shouldReturnMappedDomainWhenFound() {
            SeatReservationEntity entity = SeatReservationEntity.builder().id(ID).sagaId(SAGA_ID).build();
            SeatReservation domain = SeatReservation.restore(
                    ID, SAGA_ID, "Jan", "Mars", ReservationStatus.RESERVED, Instant.now()
            );
            when(repository.findBySagaId(SAGA_ID)).thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            Optional<SeatReservation> result = adapter.findBySagaId(SAGA_ID);

            assertThat(result).contains(domain);
        }

        @Test
        void shouldReturnEmptyWhenNotFound() {
            when(repository.findBySagaId(SAGA_ID)).thenReturn(Optional.empty());

            Optional<SeatReservation> result = adapter.findBySagaId(SAGA_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        void shouldReturnAllMappedReservations() {
            SeatReservationEntity entity = SeatReservationEntity.builder().id(ID).sagaId(SAGA_ID).build();
            SeatReservation domain = SeatReservation.restore(
                    ID, SAGA_ID, "Jan", "Mars", ReservationStatus.RESERVED, Instant.now()
            );
            when(repository.findAll()).thenReturn(List.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            List<SeatReservation> result = adapter.findAll();

            assertThat(result).containsExactly(domain);
        }

        @Test
        void shouldReturnEmptyListWhenNoEntities() {
            when(repository.findAll()).thenReturn(List.of());

            List<SeatReservation> result = adapter.findAll();

            assertThat(result).isEmpty();
        }
    }
}
