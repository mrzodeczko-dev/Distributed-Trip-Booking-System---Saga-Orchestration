package com.rzodeczko.infrastructure.persistence.entity;

import com.rzodeczko.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEntityTest {

    @Test
    void builderShouldSetAllFields() {
        UUID id = UUID.randomUUID();
        UUID sagaId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500.50");
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");

        PaymentEntity entity = PaymentEntity.builder()
                .id(id)
                .sagaId(sagaId)
                .customerName("Jan")
                .amount(amount)
                .status(PaymentStatus.CHARGED)
                .createdAt(createdAt)
                .build();

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getSagaId()).isEqualTo(sagaId);
        assertThat(entity.getCustomerName()).isEqualTo("Jan");
        assertThat(entity.getAmount()).isEqualByComparingTo(amount);
        assertThat(entity.getStatus()).isEqualTo(PaymentStatus.CHARGED);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void settersShouldUpdateFields() {
        PaymentEntity entity = new PaymentEntity();

        entity.setStatus(PaymentStatus.REFUNDED);
        entity.setAmount(new BigDecimal("42.00"));

        assertThat(entity.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(entity.getAmount()).isEqualByComparingTo("42.00");
    }
}
