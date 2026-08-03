package com.rzodeczko.infrastructure.tx;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
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
    @DisplayName("list() delegates and returns result")
    void shouldDelegateList() {
        var query = new PageQuery(0, 20);
        var dto = new PaymentDto("id", "saga", "Jan", BigDecimal.TEN, "CHARGED", Instant.now());
        var pageResult = new PageResult<>(List.of(dto), 0, 20, 1);
        when(delegate.list(query)).thenReturn(pageResult);

        PageResult<PaymentDto> result = sut.list(query);

        assertThat(result).isEqualTo(pageResult);
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
