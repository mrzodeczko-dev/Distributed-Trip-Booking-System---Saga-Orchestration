package com.rzodeczko.infrastructure.persistence.adapter;

import com.rzodeczko.domain.model.Payment;
import com.rzodeczko.domain.model.PaymentStatus;
import com.rzodeczko.infrastructure.persistence.entity.PaymentEntity;
import com.rzodeczko.infrastructure.persistence.mapper.PaymentMapper;
import com.rzodeczko.infrastructure.persistence.repository.JpaPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRepositoryAdapter")
class PaymentRepositoryAdapterTest {

    @Mock
    private JpaPaymentRepository jpaPaymentRepository;

    @Mock
    private PaymentMapper mapper;

    private PaymentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PaymentRepositoryAdapter(jpaPaymentRepository, mapper);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("maps and inserts a new entity when none exists for the id")
        void insertsNewEntity() {
            Payment payment = Payment.charge(UUID.randomUUID(), "Alice", BigDecimal.TEN);
            PaymentEntity newEntity = PaymentEntity.builder().id(payment.getId()).build();
            when(jpaPaymentRepository.findById(payment.getId())).thenReturn(Optional.empty());
            when(mapper.toEntity(payment)).thenReturn(newEntity);

            adapter.save(payment);

            verify(jpaPaymentRepository).save(newEntity);
        }

        @Test
        @DisplayName("updates the status of an existing entity instead of remapping it")
        void updatesExistingEntityStatus() {
            Payment payment = Payment.charge(UUID.randomUUID(), "Alice", BigDecimal.TEN);
            payment.refund();
            PaymentEntity existing = PaymentEntity.builder()
                    .id(payment.getId())
                    .status(PaymentStatus.CHARGED)
                    .build();
            when(jpaPaymentRepository.findById(payment.getId())).thenReturn(Optional.of(existing));

            adapter.save(payment);

            ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
            verify(jpaPaymentRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existing);
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verify(mapper, never()).toEntity(any());
        }
    }

    @Nested
    @DisplayName("existsBySagaId")
    class ExistsBySagaId {

        @Test
        @DisplayName("delegates to the JPA repository")
        void delegatesExists() {
            UUID sagaId = UUID.randomUUID();
            when(jpaPaymentRepository.existsBySagaId(sagaId)).thenReturn(true);

            boolean result = adapter.existsBySagaId(sagaId);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("findBySagaId")
    class FindBySagaId {

        @Test
        @DisplayName("maps the entity to a domain payment when found")
        void mapsFoundEntity() {
            UUID sagaId = UUID.randomUUID();
            PaymentEntity entity = PaymentEntity.builder().sagaId(sagaId).build();
            Payment domain = Payment.charge(sagaId, "Alice", BigDecimal.TEN);
            when(jpaPaymentRepository.findBySagaId(sagaId)).thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            Optional<Payment> result = adapter.findBySagaId(sagaId);

            assertThat(result).contains(domain);
        }

        @Test
        @DisplayName("returns empty optional when not found")
        void returnsEmptyWhenNotFound() {
            UUID sagaId = UUID.randomUUID();
            when(jpaPaymentRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());

            Optional<Payment> result = adapter.findBySagaId(sagaId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("maps all found entities to domain payments")
        void mapsAllEntities() {
            PaymentEntity entity = PaymentEntity.builder().id(UUID.randomUUID()).build();
            Payment domain = Payment.charge(UUID.randomUUID(), "Alice", BigDecimal.TEN);
            when(jpaPaymentRepository.findAll()).thenReturn(List.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            List<Payment> result = adapter.findAll();

            assertThat(result).containsExactly(domain);
        }
    }
}
