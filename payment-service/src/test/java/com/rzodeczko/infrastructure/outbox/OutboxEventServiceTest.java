package com.rzodeczko.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventService")
class OutboxEventServiceTest {

    @Mock
    private JpaOutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    private OutboxEventService service;

    @BeforeEach
    void setUp() {
        service = new OutboxEventService(outboxEventRepository, objectMapper);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("serializes payload and persists an unpublished outbox event")
        void savesSerializedEvent() throws JsonProcessingException {
            Object payload = new Object();
            when(objectMapper.writeValueAsString(payload)).thenReturn("{\"key\":\"value\"}");
            when(outboxEventRepository.save(any(OutboxEventEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            OutboxEventEntity result = service.save("PAYMENT_RESERVE_REPLY", payload, "ex", "rk");

            ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
            org.mockito.Mockito.verify(outboxEventRepository).save(captor.capture());
            OutboxEventEntity saved = captor.getValue();

            assertThat(saved.getEventType()).isEqualTo("PAYMENT_RESERVE_REPLY");
            assertThat(saved.getPayload()).isEqualTo("{\"key\":\"value\"}");
            assertThat(saved.getExchange()).isEqualTo("ex");
            assertThat(saved.getRoutingKey()).isEqualTo("rk");
            assertThat(saved.isPublished()).isFalse();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(result).isSameAs(saved);
        }

        @Test
        @DisplayName("wraps JsonProcessingException in OutboxSerializationException")
        void wrapsSerializationFailure() throws JsonProcessingException {
            Object payload = new Object();
            JsonProcessingException cause = new JsonProcessingException("boom") {
            };
            when(objectMapper.writeValueAsString(payload)).thenThrow(cause);

            assertThatThrownBy(() -> service.save("EVENT_TYPE", payload, "ex", "rk"))
                    .isInstanceOf(OutboxSerializationException.class)
                    .hasMessageContaining("EVENT_TYPE")
                    .hasCause(cause);
        }
    }
}
