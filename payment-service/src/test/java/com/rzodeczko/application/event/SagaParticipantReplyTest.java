package com.rzodeczko.application.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SagaParticipantReplyTest {

    @Test
    @DisplayName("from() builds reply from CommandResult success")
    void fromSuccess() {
        var sagaId = UUID.randomUUID();
        var reply = SagaParticipantReply.from(sagaId, "PAYMENT", SagaAction.RESERVE, CommandResult.success());

        assertThat(reply.sagaId()).isEqualTo(sagaId);
        assertThat(reply.step()).isEqualTo("PAYMENT");
        assertThat(reply.action()).isEqualTo(SagaAction.RESERVE);
        assertThat(reply.status()).isEqualTo("SUCCESS");
        assertThat(reply.reason()).isNull();
    }

    @Test
    @DisplayName("from() builds reply from CommandResult failure")
    void fromFailure() {
        var sagaId = UUID.randomUUID();
        var reply = SagaParticipantReply.from(sagaId, "PAYMENT", SagaAction.RESERVE, CommandResult.failure("declined"));

        assertThat(reply.status()).isEqualTo("FAILURE");
        assertThat(reply.reason()).isEqualTo("declined");
    }
}
