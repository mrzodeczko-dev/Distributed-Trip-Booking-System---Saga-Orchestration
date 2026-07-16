package com.rzodeczko.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository repository;

    private OutboxEventService service;

    @BeforeEach
    void setUp() {
        service = new OutboxEventService(repository, new ObjectMapper());
        when(repository.save(any(OutboxEventEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void saveShouldSerializePayloadAndBuildEntity() {
        record Payload(String sagaId, int value) {}
        Payload payload = new Payload("saga-1", 42);

        OutboxEventEntity saved = service.save("test.event", payload, "x.commands", "test.command");

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        OutboxEventEntity persisted = captor.getValue();

        assertThat(persisted.getEventType()).isEqualTo("test.event");
        assertThat(persisted.getExchange()).isEqualTo("x.commands");
        assertThat(persisted.getRoutingKey()).isEqualTo("test.command");
        assertThat(persisted.getPayload()).contains("\"sagaId\":\"saga-1\"").contains("\"value\":42");
        assertThat(persisted.isPublished()).isFalse();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(saved).isEqualTo(persisted);
    }

    @Test
    void saveShouldWrapJsonProcessingExceptionInOutboxSerializationException() throws Exception {
        ObjectMapper failing = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("cannot serialize") {};
            }
        };
        OutboxEventService failingService = new OutboxEventService(repository, failing);

        assertThatExceptionOfType(OutboxSerializationException.class)
                .isThrownBy(() -> failingService.save("t", new Object(), "e", "r"))
                .withMessageContaining("t");
    }
}
