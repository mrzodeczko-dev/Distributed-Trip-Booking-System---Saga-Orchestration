package com.rzodeczko.application.port.out;

import com.rzodeczko.application.event.SagaParticipantReply;

/**
 * Port wyjsciowy: publikacja odpowiedzi uczestnika do orkiestratora.
 * Implementacja zapisuje odpowiedz do Outboxa (transakcyjnie z operacja biznesowa).
 */
public interface SagaReplyPort {
    void publish(SagaParticipantReply reply);
}
