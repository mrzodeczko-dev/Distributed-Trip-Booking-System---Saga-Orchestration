package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.dto.PaymentDto;
import com.rzodeczko.application.port.in.GetPaymentUseCase;
import com.rzodeczko.presentation.dto.response.PaymentResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController")
class PaymentControllerTest {

    @Mock
    private GetPaymentUseCase getPaymentUseCase;

    private PaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(getPaymentUseCase);
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("returns 200 OK with the mapped payment list")
        void returnsMappedList() {
            PaymentDto dto = new PaymentDto(
                    UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                    "Alice", BigDecimal.TEN, "CHARGED", Instant.now());
            when(getPaymentUseCase.listAll()).thenReturn(List.of(dto));

            ResponseEntity<List<PaymentResponseDto>> response = controller.listAll();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().getFirst().customerName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("returns 200 OK with an empty list when there are no payments")
        void returnsEmptyList() {
            when(getPaymentUseCase.listAll()).thenReturn(List.of());

            ResponseEntity<List<PaymentResponseDto>> response = controller.listAll();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBySaga")
    class GetBySaga {

        @Test
        @DisplayName("returns 200 OK with the payment when found")
        void returnsPaymentWhenFound() {
            UUID sagaId = UUID.randomUUID();
            PaymentDto dto = new PaymentDto(
                    UUID.randomUUID().toString(), sagaId.toString(),
                    "Bob", BigDecimal.valueOf(50), "REFUNDED", Instant.now());
            when(getPaymentUseCase.getBySagaId(sagaId)).thenReturn(Optional.of(dto));

            ResponseEntity<PaymentResponseDto> response = controller.getBySaga(sagaId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().sagaId()).isEqualTo(sagaId.toString());
        }

        @Test
        @DisplayName("returns 404 Not Found when the payment does not exist")
        void returns404WhenNotFound() {
            UUID sagaId = UUID.randomUUID();
            when(getPaymentUseCase.getBySagaId(sagaId)).thenReturn(Optional.empty());

            ResponseEntity<PaymentResponseDto> response = controller.getBySaga(sagaId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNull();
        }
    }
}
