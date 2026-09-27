package com.rzodeczko.infrastructure.tx;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.SagaInstanceDto;
import com.rzodeczko.application.dto.SagaStepDto;
import com.rzodeczko.application.dto.StuckSagaDto;
import com.rzodeczko.application.dto.StuckSagaQuery;
import com.rzodeczko.application.service.SagaQueryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionalSagaQueryServiceTest {

    @Mock
    private SagaQueryServiceImpl delegate;

    @InjectMocks
    private TransactionalSagaQueryService service;

    private SagaInstanceDto sampleDto(String id) {
        return new SagaInstanceDto(
                id, "Jan", "Mars", BigDecimal.TEN, "IN_PROGRESS",
                List.of(new SagaStepDto("FLIGHT", "PENDING", null)),
                Instant.now(), Instant.now());
    }

    @Test
    void getByIdShouldDelegate() {
        UUID sagaId = UUID.randomUUID();
        SagaInstanceDto dto = sampleDto(sagaId.toString());
        when(delegate.getById(sagaId)).thenReturn(dto);

        SagaInstanceDto result = service.getById(sagaId);

        assertThat(result).isEqualTo(dto);
        verify(delegate).getById(sagaId);
    }

    @Test
    void listShouldDelegateAndReturnPageResult() {
        PageQuery query = new PageQuery(0, 20);
        PageResult<SagaInstanceDto> page = new PageResult<>(
                List.of(sampleDto("a"), sampleDto("b")), 0, 20, 2);
        when(delegate.list(query)).thenReturn(page);

        PageResult<SagaInstanceDto> result = service.list(query);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        verify(delegate).list(query);
    }

    @Test
    void findStuckShouldDelegate() {
        StuckSagaQuery query = new StuckSagaQuery(Duration.ofMinutes(10), 50);
        when(delegate.findStuck(query)).thenReturn(List.of(new StuckSagaDto(sampleDto("a"), "RESERVE", "FLIGHT")));

        List<StuckSagaDto> result = service.findStuck(query);

        assertThat(result).hasSize(1);
        verify(delegate).findStuck(query);
    }

    @Test
    void listShouldReturnEmptyPageWhenDelegateReturnsEmpty() {
        PageQuery query = new PageQuery(0, 20);
        when(delegate.list(query)).thenReturn(new PageResult<>(List.of(), 0, 20, 0));

        PageResult<SagaInstanceDto> result = service.list(query);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }
}
