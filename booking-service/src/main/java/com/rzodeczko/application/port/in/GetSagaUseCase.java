package com.rzodeczko.application.port.in;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.SagaInstanceDto;

import java.util.UUID;

public interface GetSagaUseCase {
    SagaInstanceDto getById(UUID sagaId);
    PageResult<SagaInstanceDto> list(PageQuery query);
}
