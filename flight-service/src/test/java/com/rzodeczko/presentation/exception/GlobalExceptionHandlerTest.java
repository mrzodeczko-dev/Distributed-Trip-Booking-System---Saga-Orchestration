package com.rzodeczko.presentation.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.rzodeczko.presentation.dto.error.ErrorResponseDto;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("handle(IllegalArgumentException)")
    class HandleIllegalArgumentException {

        @Test
        void shouldReturnBadRequestWithMessage() {
            IllegalArgumentException exception = new IllegalArgumentException("invalid sagaId");

            ResponseEntity<ErrorResponseDto> response = handler.handle(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ErrorResponseDto body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo(400);
            assertThat(body.error()).isEqualTo("Bad Request");
            assertThat(body.message()).isEqualTo("invalid sagaId");
            assertThat(body.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handle(Exception)")
    class HandleGenericException {

        @Test
        void shouldReturnInternalServerErrorWithGenericMessage() {
            Exception exception = new RuntimeException("something exploded");

            ResponseEntity<ErrorResponseDto> response = handler.handle(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            ErrorResponseDto body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo(500);
            assertThat(body.error()).isEqualTo("Internal Server Error");
            assertThat(body.message()).isEqualTo("An unexpected error");
        }
    }
}
