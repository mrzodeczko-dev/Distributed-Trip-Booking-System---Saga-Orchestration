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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<OutboxEvent> eventCaptor;

    @InjectMocks
    private OutboxEventService outboxEventService;

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        void shouldSerializePayloadAndSaveEvent() throws JsonProcessingException {
            Object payload = Map.of("key", "value");
            when(objectMapper.writeValueAsString(payload)).thenReturn("{\"key\":\"value\"}");
            when(outboxEventRepository.save(any(OutboxEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            OutboxEvent result = outboxEventService.save(
                    "FLIGHT_RESERVE", payload, "commands-exchange", "flight.reserve"
            );

            verify(outboxEventRepository).save(eventCaptor.capture());
            OutboxEvent saved = eventCaptor.getValue();
            assertThat(saved.getEventType()).isEqualTo("FLIGHT_RESERVE");
            assertThat(saved.getPayload()).isEqualTo("{\"key\":\"value\"}");
            assertThat(saved.getExchange()).isEqualTo("commands-exchange");
            assertThat(saved.getRoutingKey()).isEqualTo("flight.reserve");
            assertThat(saved.isPublished()).isFalse();
            assertThat(saved.getCreatedAt()).isNotNull();

            assertThat(result).isSameAs(saved);
        }

        @Test
        void shouldThrowOutboxSerializationExceptionWhenSerializationFails() throws JsonProcessingException {
            Object payload = Map.of("key", "value");
            JsonProcessingException cause = new JsonProcessingException("boom") {
            };
            when(objectMapper.writeValueAsString(payload)).thenThrow(cause);

            assertThatExceptionOfType(OutboxSerializationException.class)
                    .isThrownBy(() -> outboxEventService.save(
                            "FLIGHT_RESERVE", payload, "commands-exchange", "flight.reserve"
                    ))
                    .withCause(cause)
                    .withMessageContaining("FLIGHT_RESERVE");

            verify(outboxEventRepository, org.mockito.Mockito.never()).save(any());
        }
    }
}
