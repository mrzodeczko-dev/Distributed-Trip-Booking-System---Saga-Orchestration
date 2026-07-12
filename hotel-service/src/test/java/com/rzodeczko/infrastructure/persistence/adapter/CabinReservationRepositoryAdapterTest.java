package com.rzodeczko.infrastructure.persistence.adapter;

import com.rzodeczko.domain.model.CabinReservation;
import com.rzodeczko.domain.model.ReservationStatus;
import com.rzodeczko.infrastructure.persistence.entity.CabinReservationEntity;
import com.rzodeczko.infrastructure.persistence.mapper.CabinReservationMapper;
import com.rzodeczko.infrastructure.persistence.repository.JpaCabinReservationRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CabinReservationRepositoryAdapterTest {

    @Mock
    private JpaCabinReservationRepository repository;

    @Mock
    private CabinReservationMapper mapper;

    @Captor
    private ArgumentCaptor<CabinReservationEntity> entityCaptor;

    private CabinReservationRepositoryAdapter adapter;

    private static final UUID ID = UUID.randomUUID();
    private static final UUID SAGA_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new CabinReservationRepositoryAdapter(repository, mapper);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        void shouldInsertNewEntityWhenNotExisting() {
            CabinReservation reservation = CabinReservation.restore(
                    ID, SAGA_ID, "Jan", "Venus", ReservationStatus.RESERVED, Instant.now()
            );
            CabinReservationEntity mappedEntity = CabinReservationEntity.builder().id(ID).build();
            when(repository.findById(ID)).thenReturn(Optional.empty());
            when(mapper.toEntity(reservation)).thenReturn(mappedEntity);

            adapter.save(reservation);

            verify(repository).save(mappedEntity);
        }

        @Test
        void shouldUpdateExistingEntityStatusWithoutRemapping() {
            CabinReservation reservation = CabinReservation.restore(
                    ID, SAGA_ID, "Jan", "Venus", ReservationStatus.CANCELLED, Instant.now()
            );
            CabinReservationEntity existing = CabinReservationEntity.builder()
                    .id(ID)
                    .status(ReservationStatus.RESERVED)
                    .build();
            when(repository.findById(ID)).thenReturn(Optional.of(existing));

            adapter.save(reservation);

            verify(repository).save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            verify(mapper, never()).toEntity(any());
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
        void shouldMapEntityToDomainWhenFound() {
            CabinReservationEntity entity = CabinReservationEntity.builder().id(ID).build();
            CabinReservation domain = CabinReservation.restore(
                    ID, SAGA_ID, "Jan", "Venus", ReservationStatus.RESERVED, Instant.now()
            );
            when(repository.findBySagaId(SAGA_ID)).thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            Optional<CabinReservation> result = adapter.findBySagaId(SAGA_ID);

            assertThat(result).contains(domain);
        }

        @Test
        void shouldReturnEmptyWhenNotFound() {
            when(repository.findBySagaId(SAGA_ID)).thenReturn(Optional.empty());

            Optional<CabinReservation> result = adapter.findBySagaId(SAGA_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        void shouldMapAllEntitiesToDomain() {
            CabinReservationEntity entity1 = CabinReservationEntity.builder().id(ID).build();
            CabinReservationEntity entity2 = CabinReservationEntity.builder().id(UUID.randomUUID()).build();
            CabinReservation domain1 = CabinReservation.restore(
                    ID, SAGA_ID, "Jan", "Venus", ReservationStatus.RESERVED, Instant.now()
            );
            CabinReservation domain2 = CabinReservation.restore(
                    UUID.randomUUID(), UUID.randomUUID(), "Anna", "Earth", ReservationStatus.RESERVED, Instant.now()
            );
            when(repository.findAll()).thenReturn(List.of(entity1, entity2));
            when(mapper.toDomain(entity1)).thenReturn(domain1);
            when(mapper.toDomain(entity2)).thenReturn(domain2);

            List<CabinReservation> result = adapter.findAll();

            assertThat(result).containsExactly(domain1, domain2);
        }
    }
}
