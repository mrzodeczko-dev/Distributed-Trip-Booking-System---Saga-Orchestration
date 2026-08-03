package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
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
    @DisplayName("list")
    class ListPaginated {

        @Test
        @DisplayName("maps all repository payments to DTOs")
        void mapsAllPayments() {
            Payment payment = Payment.charge(sagaId, "Alice", BigDecimal.TEN);
            PageQuery query = new PageQuery(0, 20);
            when(paymentRepository.findAll(query))
                    .thenReturn(new PageResult<>(List.of(payment), 0, 20, 1));

            PageResult<PaymentDto> result = service.list(query);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().sagaId()).isEqualTo(sagaId.toString());
            assertThat(result.content().getFirst().customerName()).isEqualTo("Alice");
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("returns empty page when repository has no payments")
        void returnsEmptyPageWhenNoPayments() {
            PageQuery query = new PageQuery(0, 20);
            when(paymentRepository.findAll(query))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0));

            PageResult<PaymentDto> result = service.list(query);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
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
