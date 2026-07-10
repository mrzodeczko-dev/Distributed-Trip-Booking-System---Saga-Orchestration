package com.rzodeczko.application.port.out;


import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStepName;

public interface SagaCommandPort {
    void sendReserve(SagaInstance saga, SagaStepName step);
    void sendCancel(SagaInstance saga, SagaStepName step);
}
