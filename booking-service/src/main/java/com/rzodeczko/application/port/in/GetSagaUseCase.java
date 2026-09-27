package com.rzodeczko.application.port.in;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.SagaInstanceDto;
import com.rzodeczko.application.dto.StuckSagaDto;
import com.rzodeczko.application.dto.StuckSagaQuery;

import java.util.List;
import java.util.UUID;

public interface GetSagaUseCase {
    SagaInstanceDto getById(UUID sagaId);
    PageResult<SagaInstanceDto> list(PageQuery query);

    /**
     * Active sagas (IN_PROGRESS / COMPENSATING) not updated for at least {@code query.idleFor()},
     * oldest first, each with the reply it is waiting for.
     */
    List<StuckSagaDto> findStuck(StuckSagaQuery query);
}
