package com.rzodeczko.infrastructure.persistence.adapter;

import com.rzodeczko.domain.model.saga.*;
import com.rzodeczko.infrastructure.persistence.entity.SagaInstanceEntity;
import com.rzodeczko.infrastructure.persistence.mapper.SagaInstanceMapper;
import com.rzodeczko.infrastructure.persistence.repository.JpaSagaInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaInstanceRepositoryAdapterTest {

    @Mock
    private JpaSagaInstanceRepository jpaRepository;
    @Mock
    private SagaInstanceMapper mapper;

    private SagaInstanceRepositoryAdapter adapter;

    private static final UUID SAGA_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new SagaInstanceRepositoryAdapter(jpaRepository, mapper);
    }

    private SagaInstance createSaga() {
        return SagaInstance.restore(
                SAGA_ID, "Jan", "Mars", BigDecimal.TEN,
                SagaStatus.IN_PROGRESS,
                List.of(SagaStep.restore(SagaStepName.FLIGHT, SagaStepStatus.PENDING, null)),
                Instant.now(), Instant.now()
        );
    }

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("should update existing entity when found by id")
        void shouldUpdateExistingEntity() {
            SagaInstance saga = createSaga();
            SagaInstanceEntity existing = SagaInstanceEntity.builder().id(SAGA_ID).build();
            when(jpaRepository.findById(SAGA_ID)).thenReturn(Optional.of(existing));

            adapter.save(saga);

            verify(mapper).updateEntity(existing, saga);
            verify(mapper, never()).toNewEntity(any());
            verify(jpaRepository).save(existing);
        }

        @Test
        @DisplayName("should create new entity when not found")
        void shouldCreateNewEntity() {
            SagaInstance saga = createSaga();
            SagaInstanceEntity newEntity = SagaInstanceEntity.builder().id(SAGA_ID).build();
            when(jpaRepository.findById(SAGA_ID)).thenReturn(Optional.empty());
            when(mapper.toNewEntity(saga)).thenReturn(newEntity);

            adapter.save(saga);

            verify(mapper).toNewEntity(saga);
            verify(mapper, never()).updateEntity(any(), any());
            verify(jpaRepository).save(newEntity);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("should return mapped domain when entity found")
        void shouldReturnMappedDomain() {
            SagaInstance saga = createSaga();
            SagaInstanceEntity entity = SagaInstanceEntity.builder().id(SAGA_ID).build();
            when(jpaRepository.findById(SAGA_ID)).thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(saga);

            Optional<SagaInstance> result = adapter.findById(SAGA_ID);

            assertThat(result).contains(saga);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmpty() {
            when(jpaRepository.findById(SAGA_ID)).thenReturn(Optional.empty());

            assertThat(adapter.findById(SAGA_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdForUpdate()")
    class FindByIdForUpdate {

        @Test
        @DisplayName("should delegate to pessimistic-lock query")
        void shouldDelegateToPessimisticLock() {
            SagaInstance saga = createSaga();
            SagaInstanceEntity entity = SagaInstanceEntity.builder().id(SAGA_ID).build();
            when(jpaRepository.findByIdForUpdate(SAGA_ID)).thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(saga);

            Optional<SagaInstance> result = adapter.findByIdForUpdate(SAGA_ID);

            assertThat(result).contains(saga);
            verify(jpaRepository).findByIdForUpdate(SAGA_ID);
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("should map all entities to domain")
        void shouldMapAll() {
            SagaInstanceEntity e1 = SagaInstanceEntity.builder().id(UUID.randomUUID()).build();
            SagaInstanceEntity e2 = SagaInstanceEntity.builder().id(UUID.randomUUID()).build();
            SagaInstance s1 = createSaga();
            SagaInstance s2 = createSaga();
            when(jpaRepository.findAll()).thenReturn(List.of(e1, e2));
            when(mapper.toDomain(e1)).thenReturn(s1);
            when(mapper.toDomain(e2)).thenReturn(s2);

            List<SagaInstance> result = adapter.findAll();

            assertThat(result).containsExactly(s1, s2);
        }

        @Test
        @DisplayName("should return empty list when no entities")
        void shouldReturnEmptyList() {
            when(jpaRepository.findAll()).thenReturn(List.of());

            assertThat(adapter.findAll()).isEmpty();
        }
    }
}
