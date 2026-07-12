package com.rzodeczko.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private JpaOutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<OutboxEventEntity> entityCaptor;

    private OutboxEventService service;

    private record SamplePayload(String value) {
    }

    @BeforeEach
    void setUp() {
        service = new OutboxEventService(outboxEventRepository, objectMapper);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        void shouldSerializePayloadAndPersistEvent() throws JsonProcessingException {
            SamplePayload payload = new SamplePayload("hello");
            when(objectMapper.writeValueAsString(payload)).thenReturn("{\"value\":\"hello\"}");
            when(outboxEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            OutboxEventEntity result = service.save("FLIGHT_RESERVE_REPLY", payload, "x.saga.replies", "flight.reply");

            verify(outboxEventRepository).save(entityCaptor.capture());
            OutboxEventEntity saved = entityCaptor.getValue();
            assertThat(saved.getEventType()).isEqualTo("FLIGHT_RESERVE_REPLY");
            assertThat(saved.getPayload()).isEqualTo("{\"value\":\"hello\"}");
            assertThat(saved.getExchange()).isEqualTo("x.saga.replies");
            assertThat(saved.getRoutingKey()).isEqualTo("flight.reply");
            assertThat(saved.isPublished()).isFalse();
            assertThat(saved.getCreatedAt()).isNotNull();

            assertThat(result).isEqualTo(saved);
        }

        @Test
        void shouldThrowOutboxSerializationExceptionWhenSerializationFails() throws JsonProcessingException {
            SamplePayload payload = new SamplePayload("boom");
            when(objectMapper.writeValueAsString(payload))
                    .thenThrow(new JsonProcessingException("failure") {
                    });

            assertThatThrownBy(() -> service.save("EVENT_TYPE", payload, "exchange", "routingKey"))
                    .isInstanceOf(OutboxSerializationException.class)
                    .hasMessageContaining("EVENT_TYPE");

            verify(outboxEventRepository, org.mockito.Mockito.never()).save(any());
        }
    }
}
