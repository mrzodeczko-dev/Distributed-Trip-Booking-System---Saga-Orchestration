package com.rzodeczko.application.port.out;

import com.rzodeczko.application.event.SagaParticipantReply;

public interface SagaReplyPort {
    void publish(SagaParticipantReply reply);
}
