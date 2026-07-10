package com.rzodeczko.application.port.in;


import com.rzodeczko.application.event.SagaReply;

public interface HandleSagaReplyUseCase {
    void handle(SagaReply reply);
}
