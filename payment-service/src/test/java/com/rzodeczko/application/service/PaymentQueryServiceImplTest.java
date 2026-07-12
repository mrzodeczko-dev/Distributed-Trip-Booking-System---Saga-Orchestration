package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.PaymentDto;
import com.rzodeczko.application.port.out.PaymentRepository;
import com.rzodeczko.domain.model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentQueryServiceImpl")
class PaymentQueryServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentQueryServiceImpl service;

    private UUID sagaId;

    @BeforeEach
    void setUp() {
        service = new PaymentQueryServiceImpl(paymentRepository);
        sagaId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("listAll")
    class ListAll {

        @Test
        @DisplayName("maps all repository payments to DTOs")
        void mapsAllPayments() {
            Payment payment = Payment.charge(sagaId, "Alice", BigDecimal.TEN);
            when(paymentRepository.findAll()).thenReturn(List.of(payment));

            List<PaymentDto> result = service.listAll();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().sagaId()).isEqualTo(sagaId.toString());
            assertThat(result.getFirst().customerName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("returns empty list when repository has no payments")
        void returnsEmptyListWhenNoPayments() {
            when(paymentRepository.findAll()).thenReturn(List.of());

            List<PaymentDto> result = service.listAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBySagaId")
    class GetBySagaId {

        @Test
        @DisplayName("returns mapped DTO when payment exists")
        void returnsDtoWhenFound() {
            Payment payment = Payment.charge(sagaId, "Alice", BigDecimal.TEN);
            when(paymentRepository.findBySagaId(sagaId)).thenReturn(Optional.of(payment));

            Optional<PaymentDto> result = service.getBySagaId(sagaId);

            assertThat(result).isPresent();
            assertThat(result.get().sagaId()).isEqualTo(sagaId.toString());
        }

        @Test
        @DisplayName("returns empty optional when payment does not exist")
        void returnsEmptyWhenNotFound() {
            when(paymentRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());

            Optional<PaymentDto> result = service.getBySagaId(sagaId);

            assertThat(result).isEmpty();
        }
    }
}
