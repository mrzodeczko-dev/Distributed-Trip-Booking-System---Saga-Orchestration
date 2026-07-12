package com.rzodeczko.infrastructure.persistence.mapper;

import com.rzodeczko.domain.model.Payment;
import com.rzodeczko.domain.model.PaymentStatus;
import com.rzodeczko.infrastructure.persistence.entity.PaymentEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentMapper")
class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("maps all domain fields onto the entity")
        void mapsDomainToEntity() {
            Payment payment = Payment.charge(UUID.randomUUID(), "Alice", BigDecimal.valueOf(200));

            PaymentEntity entity = mapper.toEntity(payment);

            assertThat(entity.getId()).isEqualTo(payment.getId());
            assertThat(entity.getSagaId()).isEqualTo(payment.getSagaId());
            assertThat(entity.getCustomerName()).isEqualTo(payment.getCustomerName());
            assertThat(entity.getAmount()).isEqualByComparingTo(payment.getAmount());
            assertThat(entity.getStatus()).isEqualTo(payment.getStatus());
            assertThat(entity.getCreatedAt()).isEqualTo(payment.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("maps all entity fields onto the domain model")
        void mapsEntityToDomain() {
            UUID id = UUID.randomUUID();
            UUID sagaId = UUID.randomUUID();
            Instant createdAt = Instant.parse("2024-05-01T10:00:00Z");
            PaymentEntity entity = PaymentEntity.builder()
                    .id(id)
                    .sagaId(sagaId)
                    .customerName("Bob")
                    .amount(BigDecimal.valueOf(300))
                    .status(PaymentStatus.REFUNDED)
                    .createdAt(createdAt)
                    .build();

            Payment payment = mapper.toDomain(entity);

            assertThat(payment.getId()).isEqualTo(id);
            assertThat(payment.getSagaId()).isEqualTo(sagaId);
            assertThat(payment.getCustomerName()).isEqualTo("Bob");
            assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(300));
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.getCreatedAt()).isEqualTo(createdAt);
        }
    }
}
