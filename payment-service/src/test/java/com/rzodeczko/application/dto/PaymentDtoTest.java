package com.rzodeczko.application.dto;

import com.rzodeczko.domain.model.Payment;
import com.rzodeczko.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDtoTest {

    @Test
    @DisplayName("from() maps all domain fields to DTO")
    void shouldMapFromDomain() {
        var id = UUID.randomUUID();
        var sagaId = UUID.randomUUID();
        var now = Instant.now();
        var payment = Payment.builder()
                .id(id)
                .sagaId(sagaId)
                .customerName("Jan")
                .amount(BigDecimal.valueOf(250))
                .status(PaymentStatus.CHARGED)
                .createdAt(now)
                .build();

        var dto = PaymentDto.from(payment);

        assertThat(dto.id()).isEqualTo(id.toString());
        assertThat(dto.sagaId()).isEqualTo(sagaId.toString());
        assertThat(dto.customerName()).isEqualTo("Jan");
        assertThat(dto.amount()).isEqualByComparingTo(BigDecimal.valueOf(250));
        assertThat(dto.status()).isEqualTo("CHARGED");
        assertThat(dto.createdAt()).isEqualTo(now);
    }
}
