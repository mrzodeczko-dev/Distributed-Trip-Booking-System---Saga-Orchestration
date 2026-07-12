package com.rzodeczko.infrastructure.outbox;

import com.rzodeczko.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    @Transactional
    void shouldSaveOutboxEvent() {
        Map<String, Object> payload = Map.of("sagaId", "123", "action", "RESERVE");

        OutboxEvent saved = outboxEventService.save(
                "FLIGHT_RESERVE", payload, "x.saga.commands", "flight.command");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventType()).isEqualTo("FLIGHT_RESERVE");
        assertThat(saved.getExchange()).isEqualTo("x.saga.commands");
        assertThat(saved.getRoutingKey()).isEqualTo("flight.command");
        assertThat(saved.isPublished()).isFalse();
        assertThat(saved.getPayload()).contains("sagaId");
    }

    @Test
    @Transactional
    void shouldThrowOutboxSerializationExceptionForUnserializable() {
        Object unserializable = new Object() {
            // Jackson can't serialize this without accessors
            private final Object self = this; // circular reference
        };

        assertThatThrownBy(() ->
                outboxEventService.save("BAD", unserializable, "x", "k"))
                .isInstanceOf(OutboxSerializationException.class);
    }
}
