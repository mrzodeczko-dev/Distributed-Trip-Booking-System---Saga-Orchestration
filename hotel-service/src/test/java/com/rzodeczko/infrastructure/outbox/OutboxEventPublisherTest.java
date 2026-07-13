package com.rzodeczko.infrastructure.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private JpaOutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Captor
    private ArgumentCaptor<OutboxEventEntity> entityCaptor;

    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxEventPublisher(outboxEventRepository, rabbitTemplate, new ObjectMapper(), 5);
    }

    @Nested
    @DisplayName("publishPendingEvents")
    class PublishPendingEvents {

        @Test
        void shouldDoNothingWhenNoUnpublishedEvents() {
            when(outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc()).thenReturn(List.of());

            publisher.publishPendingEvents();

            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
            verify(outboxEventRepository, never()).save(any());
        }

        @Test
        void shouldPublishEventAndMarkSuccess() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .id("id-1")
                    .eventType("HOTEL_RESERVE_REPLY")
                    .payload("{}")
                    .exchange("exchange")
                    .routingKey("routing-key")
                    .createdAt(LocalDateTime.now())
                    .published(false)
                    .attemptCount(0)
                    .build();
            when(outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                    .thenReturn(List.of(event));

            publisher.publishPendingEvents();

            verify(rabbitTemplate).send(eq("exchange"), eq("routing-key"), any(Message.class));
            verify(outboxEventRepository).save(entityCaptor.capture());
            OutboxEventEntity saved = entityCaptor.getValue();
            assertThat(saved.isPublished()).isTrue();
            assertThat(saved.getPublishedAt()).isNotNull();
            assertThat(saved.getAttemptCount()).isEqualTo(1);
        }

        @Test
        void shouldMarkFailureWhenPublishThrows() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .id("id-1")
                    .eventType("HOTEL_RESERVE_REPLY")
                    .payload("{}")
                    .exchange("exchange")
                    .routingKey("routing-key")
                    .createdAt(LocalDateTime.now())
                    .published(false)
                    .attemptCount(0)
                    .build();
            when(outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                    .thenReturn(List.of(event));
            doThrow(new RuntimeException("broker down"))
                    .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

            publisher.publishPendingEvents();

            verify(outboxEventRepository).save(entityCaptor.capture());
            OutboxEventEntity saved = entityCaptor.getValue();
            assertThat(saved.isPublished()).isFalse();
            assertThat(saved.getLastError()).isEqualTo("broker down");
            assertThat(saved.getAttemptCount()).isEqualTo(1);
        }

        @Test
        void shouldDeadLetterEventWhenMaxAttemptsExceeded() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .id("evt-poison")
                    .eventType("HOTEL_RESERVE_REPLY")
                    .payload("{}")
                    .exchange("exchange")
                    .routingKey("routing-key")
                    .createdAt(LocalDateTime.now())
                    .published(false)
                    .attemptCount(5)
                    .lastError("broker down")
                    .build();
            when(outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                    .thenReturn(List.of(event));

            publisher.publishPendingEvents();

            verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
            verify(outboxEventRepository).save(entityCaptor.capture());
            OutboxEventEntity saved = entityCaptor.getValue();
            assertThat(saved.isDeadLettered()).isTrue();
            assertThat(saved.getDeadLetteredAt()).isNotNull();
            assertThat(saved.isPublished()).isFalse();
        }

        @Test
        void shouldNotDeadLetterEventBelowMaxAttempts() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .id("evt-retry")
                    .eventType("HOTEL_RESERVE_REPLY")
                    .payload("{}")
                    .exchange("exchange")
                    .routingKey("routing-key")
                    .createdAt(LocalDateTime.now())
                    .published(false)
                    .attemptCount(4)
                    .build();
            when(outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                    .thenReturn(List.of(event));

            publisher.publishPendingEvents();

            verify(rabbitTemplate).send(anyString(), anyString(), any(Message.class));
            verify(outboxEventRepository).save(entityCaptor.capture());
            OutboxEventEntity saved = entityCaptor.getValue();
            assertThat(saved.isDeadLettered()).isFalse();
        }
    }
}
