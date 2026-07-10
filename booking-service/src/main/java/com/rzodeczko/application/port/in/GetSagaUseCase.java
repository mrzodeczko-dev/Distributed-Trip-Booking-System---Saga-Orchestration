package com.rzodeczko.application.port.in;


import com.rzodeczko.application.dto.SagaInstanceDto;

import java.util.List;
import java.util.UUID;

public interface GetSagaUseCase {
    SagaInstanceDto getById(UUID sagaId);
    List<SagaInstanceDto> listAll();
}
