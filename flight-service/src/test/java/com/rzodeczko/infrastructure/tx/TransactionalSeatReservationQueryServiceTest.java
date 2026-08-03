package com.rzodeczko.infrastructure.tx;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.SeatReservationDto;
import com.rzodeczko.application.service.SeatReservationQueryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionalSeatReservationQueryServiceTest {

    @Mock
    private SeatReservationQueryServiceImpl delegate;

    @InjectMocks
    private TransactionalSeatReservationQueryService service;

    private SeatReservationDto sampleDto(String customer) {
        return new SeatReservationDto("id", "saga", customer, "Mars", "RESERVED", Instant.now());
    }

    @Test
    void listShouldDelegateAndReturnResult() {
        PageQuery pageQuery = new PageQuery(0, 20);
        SeatReservationDto dto = sampleDto("Jan");
        PageResult<SeatReservationDto> pageResult = new PageResult<>(List.of(dto), 0, 20, 1);
        when(delegate.list(pageQuery)).thenReturn(pageResult);

        PageResult<SeatReservationDto> result = service.list(pageQuery);

        assertThat(result.content()).containsExactly(dto);
        assertThat(result.totalElements()).isEqualTo(1);
        verify(delegate).list(pageQuery);
    }

    @Test
    void listShouldReturnEmptyPageWhenDelegateReturnsEmpty() {
        PageQuery pageQuery = new PageQuery(0, 20);
        PageResult<SeatReservationDto> pageResult = new PageResult<>(List.of(), 0, 20, 0);
        when(delegate.list(pageQuery)).thenReturn(pageResult);

        PageResult<SeatReservationDto> result = service.list(pageQuery);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    void getBySagaIdShouldDelegateAndReturnResult() {
        UUID sagaId = UUID.randomUUID();
        SeatReservationDto dto = sampleDto("Anna");
        when(delegate.getBySagaId(sagaId)).thenReturn(Optional.of(dto));

        Optional<SeatReservationDto> result = service.getBySagaId(sagaId);

        assertThat(result).contains(dto);
        verify(delegate).getBySagaId(sagaId);
    }

    @Test
    void getBySagaIdShouldReturnEmptyWhenReservationNotFound() {
        UUID sagaId = UUID.randomUUID();
        when(delegate.getBySagaId(sagaId)).thenReturn(Optional.empty());

        assertThat(service.getBySagaId(sagaId)).isEmpty();
    }
}
