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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private JpaOutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<OutboxEventEntity> entityCaptor;

    private OutboxEventService service;

    @BeforeEach
    void setUp() {
        service = new OutboxEventService(outboxEventRepository, objectMapper);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        void shouldSerializePayloadAndPersistEvent() throws JsonProcessingException {
            Object payload = new Object();
            when(objectMapper.writeValueAsString(payload)).thenReturn("{\"foo\":\"bar\"}");
            when(outboxEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            OutboxEventEntity result = service.save("HOTEL_RESERVE_REPLY", payload, "exchange", "routing-key");

            verify(outboxEventRepository).save(entityCaptor.capture());
            OutboxEventEntity saved = entityCaptor.getValue();
            assertThat(saved.getEventType()).isEqualTo("HOTEL_RESERVE_REPLY");
            assertThat(saved.getPayload()).isEqualTo("{\"foo\":\"bar\"}");
            assertThat(saved.getExchange()).isEqualTo("exchange");
            assertThat(saved.getRoutingKey()).isEqualTo("routing-key");
            assertThat(saved.isPublished()).isFalse();
            assertThat(saved.getCreatedAt()).isNotNull();

            assertThat(result).isSameAs(saved);
        }

        @Test
        void shouldThrowOutboxSerializationExceptionWhenSerializationFails() throws JsonProcessingException {
            Object payload = new Object();
            JsonProcessingException cause = mock(JsonProcessingException.class);
            when(objectMapper.writeValueAsString(payload)).thenThrow(cause);

            assertThatThrownBy(() -> service.save("HOTEL_RESERVE_REPLY", payload, "exchange", "routing-key"))
                    .isInstanceOf(OutboxSerializationException.class)
                    .hasMessageContaining("HOTEL_RESERVE_REPLY")
                    .hasCause(cause);

            verify(outboxEventRepository, never()).save(any());
        }
    }
}
