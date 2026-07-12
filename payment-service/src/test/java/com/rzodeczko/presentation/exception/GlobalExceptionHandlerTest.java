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
    @DisplayName("IllegalArgumentException handler")
    class IllegalArgumentHandling {

        @Test
        @DisplayName("maps to 400 Bad Request with the exception message")
        void mapsToBadRequest() {
            IllegalArgumentException exception = new IllegalArgumentException("invalid input");

            ResponseEntity<ErrorResponseDto> response = handler.handle(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().error()).isEqualTo("Bad Request");
            assertThat(response.getBody().message()).isEqualTo("invalid input");
            assertThat(response.getBody().timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("generic Exception handler")
    class GenericExceptionHandling {

        @Test
        @DisplayName("maps to 500 Internal Server Error with a generic message, hiding exception details")
        void mapsToInternalServerError() {
            Exception exception = new RuntimeException("some sensitive internal detail");

            ResponseEntity<ErrorResponseDto> response = handler.handle(exception);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
            assertThat(response.getBody().message()).isEqualTo("Unexpected error");
            assertThat(response.getBody().message()).doesNotContain("some sensitive internal detail");
        }
    }
}
