package com.rzodeczko.presentation.dto.response;

import com.rzodeczko.application.dto.CabinReservationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CabinReservationResponseDtoTest {

    @Test
    @DisplayName("from() maps all fields from CabinReservationDto")
    void shouldMapFromDto() {
        var now = Instant.now();
        var dto = new CabinReservationDto("id-1", "saga-1", "Jan", "Zakopane", "RESERVED", now);

        var response = CabinReservationResponseDto.from(dto);

        assertThat(response.id()).isEqualTo("id-1");
        assertThat(response.sagaId()).isEqualTo("saga-1");
        assertThat(response.customerName()).isEqualTo("Jan");
        assertThat(response.destination()).isEqualTo("Zakopane");
        assertThat(response.status()).isEqualTo("RESERVED");
        assertThat(response.createdAt()).isEqualTo(now);
    }
}
