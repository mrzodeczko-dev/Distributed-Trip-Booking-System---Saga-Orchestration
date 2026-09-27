package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.StuckSagaDto;
import com.rzodeczko.application.dto.StuckSagaQuery;
import com.rzodeczko.application.port.out.SagaInstanceRepository;
import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SagaQueryServiceImplFindStuckTest {

    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");

    @Mock
    private SagaInstanceRepository sagaInstanceRepository;

    private SagaQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        queryService = new SagaQueryServiceImpl(sagaInstanceRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldQueryActiveStatusesWithCutoffAndLimit() {
        when(sagaInstanceRepository.findStuck(any(), any(), anyInt())).thenReturn(List.of());

        queryService.findStuck(new StuckSagaQuery(Duration.ofMinutes(15), 25));

        verify(sagaInstanceRepository).findStuck(
                Set.of(SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING),
                NOW.minus(Duration.ofMinutes(15)),
                25);
    }

    @Test
    void shouldMapSagasToDtosWithAwaitedReply() {
        SagaInstance saga = SagaInstance.start("Jan", "Mars", new BigDecimal("100.00"));
        when(sagaInstanceRepository.findStuck(any(), any(), anyInt())).thenReturn(List.of(saga));

        List<StuckSagaDto> result = queryService.findStuck(new StuckSagaQuery(Duration.ofMinutes(10), 50));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().saga().sagaId()).isEqualTo(saga.getId().toString());
        assertThat(result.getFirst().saga().status()).isEqualTo("IN_PROGRESS");
        assertThat(result.getFirst().waitingFor()).isEqualTo("RESERVE FLIGHT");
    }

    @Test
    void shouldReturnEmptyListWhenNothingIsStuck() {
        when(sagaInstanceRepository.findStuck(any(), any(), anyInt())).thenReturn(List.of());

        assertThat(queryService.findStuck(new StuckSagaQuery(Duration.ofMinutes(10), 50))).isEmpty();
    }

    @Test
    void activeStatusesShouldNotContainTerminalStatuses() {
        assertThat(SagaQueryServiceImpl.ACTIVE_STATUSES)
                .containsExactlyInAnyOrder(SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING);
    }
}
