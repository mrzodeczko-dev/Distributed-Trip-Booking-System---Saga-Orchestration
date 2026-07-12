package com.rzodeczko.presentation.dto.response;

import com.rzodeczko.application.dto.PaymentDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentResponseDtoTest {

    @Test
    @DisplayName("from() maps all fields from PaymentDto")
    void shouldMapFromDto() {
        var now = Instant.now();
        var dto = new PaymentDto("id-1", "saga-1", "Jan", BigDecimal.valueOf(100), "CHARGED", now);

        var response = PaymentResponseDto.from(dto);

        assertThat(response.id()).isEqualTo("id-1");
        assertThat(response.sagaId()).isEqualTo("saga-1");
        assertThat(response.customerName()).isEqualTo("Jan");
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(response.status()).isEqualTo("CHARGED");
        assertThat(response.createdAt()).isEqualTo(now);
    }
}
