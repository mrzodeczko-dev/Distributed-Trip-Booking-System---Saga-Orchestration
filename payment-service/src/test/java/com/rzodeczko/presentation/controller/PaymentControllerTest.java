package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.PaymentDto;
import com.rzodeczko.application.port.in.GetPaymentUseCase;
import com.rzodeczko.presentation.dto.response.PagedResponseDto;
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
    @DisplayName("getPayments")
    class GetPayments {

        @Test
        @DisplayName("returns 200 OK with the mapped payment page")
        void returnsMappedPage() {
            PaymentDto dto = new PaymentDto(
                    UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                    "Alice", BigDecimal.TEN, "CHARGED", Instant.now());
            when(getPaymentUseCase.list(new PageQuery(0, 20)))
                    .thenReturn(new PageResult<>(List.of(dto), 0, 20, 1));

            ResponseEntity<PagedResponseDto<PaymentResponseDto>> response =
                    controller.getPayments(0, 20);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content()).hasSize(1);
            assertThat(response.getBody().content().getFirst().customerName()).isEqualTo("Alice");
            assertThat(response.getBody().totalElements()).isEqualTo(1);
            assertThat(response.getBody().page()).isZero();
            assertThat(response.getBody().size()).isEqualTo(20);
        }

        @Test
        @DisplayName("returns 200 OK with an empty page when there are no payments")
        void returnsEmptyPage() {
            when(getPaymentUseCase.list(new PageQuery(0, 20)))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0));

            ResponseEntity<PagedResponseDto<PaymentResponseDto>> response =
                    controller.getPayments(0, 20);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().content()).isEmpty();
            assertThat(response.getBody().totalElements()).isZero();
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
