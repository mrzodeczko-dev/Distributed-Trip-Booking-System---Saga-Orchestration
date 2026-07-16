package com.rzodeczko.presentation.dto.error;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseDtoTest {

    @Test
    void threeArgConstructorShouldSetTimestampToNow() {
        Instant before = Instant.now();
        ErrorResponseDto dto = new ErrorResponseDto(400, "Bad Request", "invalid input");
        Instant after = Instant.now();

        assertThat(dto.status()).isEqualTo(400);
        assertThat(dto.error()).isEqualTo("Bad Request");
        assertThat(dto.message()).isEqualTo("invalid input");
        assertThat(dto.timestamp()).isBetween(before, after);
    }

    @Test
    void fourArgConstructorShouldPreserveExplicitTimestamp() {
        Instant ts = Instant.parse("2026-05-01T10:00:00Z");
        ErrorResponseDto dto = new ErrorResponseDto(500, "Internal Server Error", "boom", ts);

        assertThat(dto.timestamp()).isEqualTo(ts);
    }
}
