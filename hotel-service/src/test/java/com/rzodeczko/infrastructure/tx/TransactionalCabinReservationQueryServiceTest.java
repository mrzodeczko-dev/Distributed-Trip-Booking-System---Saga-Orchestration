package com.rzodeczko.infrastructure.tx;

import com.rzodeczko.application.dto.CabinReservationDto;
import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.service.CabinReservationQueryServiceImpl;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionalCabinReservationQueryServiceTest {

    @Mock
    private CabinReservationQueryServiceImpl delegate;

    @InjectMocks
    private TransactionalCabinReservationQueryService sut;

    @Test
    @DisplayName("list() delegates and returns result")
    void shouldDelegateList() {
        var dto = new CabinReservationDto("id", "saga", "Jan", "Zakopane", "RESERVED", Instant.now());
        var query = new PageQuery(0, 20);
        var pageResult = new PageResult<>(List.of(dto), 0, 20, 1);
        when(delegate.list(query)).thenReturn(pageResult);

        PageResult<CabinReservationDto> result = sut.list(query);

        assertThat(result).isEqualTo(pageResult);
        assertThat(result.content()).containsExactly(dto);
    }

    @Test
    @DisplayName("getBySagaId() delegates and returns result")
    void shouldDelegateGetBySagaId() {
        var sagaId = UUID.randomUUID();
        var dto = new CabinReservationDto("id", sagaId.toString(), "Jan", "Zakopane", "RESERVED", Instant.now());
        when(delegate.getBySagaId(sagaId)).thenReturn(Optional.of(dto));

        Optional<CabinReservationDto> result = sut.getBySagaId(sagaId);

        assertThat(result).contains(dto);
    }
}
