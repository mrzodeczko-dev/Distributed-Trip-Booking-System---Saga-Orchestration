package com.rzodeczko.common.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxSerializationExceptionTest {

    @Test
    void shouldWrapMessageAndCause() {
        Throwable cause = new RuntimeException("root");

        OutboxSerializationException ex = new OutboxSerializationException("wrap", cause);

        assertThat(ex.getMessage()).isEqualTo("wrap");
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
