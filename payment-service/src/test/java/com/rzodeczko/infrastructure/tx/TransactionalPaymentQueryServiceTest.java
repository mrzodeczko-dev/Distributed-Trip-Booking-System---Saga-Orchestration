package com.rzodeczko.infrastructure.tx;

import com.rzodeczko.application.dto.PaymentDto;
import com.rzodeczko.application.service.PaymentQueryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionalPaymentQueryServiceTest {

    @Mock
    private PaymentQueryServiceImpl delegate;

    @InjectMocks
    private TransactionalPaymentQueryService sut;

    @Test
    @DisplayName("listAll() delegates and returns result")
    void shouldDelegateListAll() {
        var dto = new PaymentDto("id", "saga", "Jan", BigDecimal.TEN, "CHARGED", Instant.now());
        when(delegate.listAll()).thenReturn(List.of(dto));

        List<PaymentDto> result = sut.listAll();

        assertThat(result).containsExactly(dto);
    }

    @Test
    @DisplayName("getBySagaId() delegates and returns result")
    void shouldDelegateGetBySagaId() {
        var sagaId = UUID.randomUUID();
        var dto = new PaymentDto("id", sagaId.toString(), "Jan", BigDecimal.TEN, "CHARGED", Instant.now());
        when(delegate.getBySagaId(sagaId)).thenReturn(Optional.of(dto));

        Optional<PaymentDto> result = sut.getBySagaId(sagaId);

        assertThat(result).contains(dto);
    }
}
