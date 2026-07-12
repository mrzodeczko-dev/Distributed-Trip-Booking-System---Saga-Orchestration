package com.rzodeczko.presentation.exception;

import com.rzodeczko.presentation.dto.error.ErrorResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("IllegalArgumentException handler")
    class IllegalArgumentExceptionHandling {

        @Test
        void shouldReturnBadRequestWithMessage() {
            IllegalArgumentException exception = new IllegalArgumentException("invalid sagaId");

            ResponseEntity<ErrorResponseDto> response = handler.handle(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().error()).isEqualTo("Bad Request");
            assertThat(response.getBody().message()).isEqualTo("invalid sagaId");
            assertThat(response.getBody().timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Generic Exception handler")
    class GenericExceptionHandling {

        @Test
        void shouldReturnInternalServerErrorWithGenericMessage() {
            Exception exception = new RuntimeException("boom");

            ResponseEntity<ErrorResponseDto> response = handler.handle(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
            assertThat(response.getBody().message()).isEqualTo("Unexpected error");
        }
    }
}
