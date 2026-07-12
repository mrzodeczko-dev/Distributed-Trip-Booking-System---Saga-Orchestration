package com.rzodeczko.presentation.exception;

import com.rzodeczko.domain.exception.InvalidSagaStateException;
import com.rzodeczko.domain.exception.SagaNotFoundException;
import com.rzodeczko.presentation.dto.error.ErrorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("SagaNotFoundException")
    class SagaNotFound {

        @Test
        @DisplayName("should return 404 with saga id in message")
        void shouldReturn404() {
            UUID sagaId = UUID.randomUUID();
            SagaNotFoundException ex = new SagaNotFoundException(sagaId);

            ResponseEntity<ErrorResponseDto> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().error()).isEqualTo("Not found");
            assertThat(response.getBody().message()).contains(sagaId.toString());
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException / InvalidSagaStateException")
    class BadRequest {

        @Test
        @DisplayName("should return 400 for IllegalArgumentException")
        void shouldReturn400ForIllegalArgument() {
            IllegalArgumentException ex = new IllegalArgumentException("bad input");

            ResponseEntity<ErrorResponseDto> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().message()).isEqualTo("bad input");
        }

        @Test
        @DisplayName("should return 400 for InvalidSagaStateException")
        void shouldReturn400ForInvalidSagaState() {
            InvalidSagaStateException ex = new InvalidSagaStateException("invalid state");

            ResponseEntity<ErrorResponseDto> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).isEqualTo("invalid state");
        }
    }

    @Nested
    @DisplayName("Generic Exception")
    class GenericException {

        @Test
        @DisplayName("should return 500 with generic message")
        void shouldReturn500() {
            Exception ex = new Exception("something broke");

            ResponseEntity<ErrorResponseDto> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().message()).isEqualTo("An unexpected error");
        }
    }
}
