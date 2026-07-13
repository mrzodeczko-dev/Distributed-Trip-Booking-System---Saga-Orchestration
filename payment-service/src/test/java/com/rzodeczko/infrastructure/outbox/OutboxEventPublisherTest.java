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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventPublisher")
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
        @DisplayName("does nothing when there are no unpublished events")
        void doesNothingWhenNoUnpublishedEvents() {
            when(outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc()).thenReturn(List.of());

            publisher.publishPendingEvents();

            verifyNoInteractions(rabbitTemplate);
            verify(outboxEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("publishes each pending event and marks it as published")
        void publishesPendingEvents() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .id("event-1")
                    .eventType("PAYMENT_RESERVE_REPLY")
                    .payload("{}")
                    .exchange("ex")
                    .routingKey("rk")
                    .createdAt(LocalDateTime.now())
                    .published(false)
                    .attemptCount(0)
                    .build();
            when(outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                    .thenReturn(List.of(event));

            publisher.publishPendingEvents();

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(eq("ex"), eq("rk"), messageCaptor.capture());
            assertThat(new String(messageCaptor.getValue().getBody())).isEqualTo("{}");

            assertThat(event.isPublished()).isTrue();
            assertThat(event.getAttemptCount()).isEqualTo(1);
            verify(outboxEventRepository).save(event);
        }

        @Test
        @DisplayName("marks the event as failed and records the error when publishing throws")
        void marksEventAsFailedOnError() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .id("event-2")
                    .eventType("PAYMENT_RESERVE_REPLY")
                    .payload("{}")
                    .exchange("ex")
                    .routingKey("rk")
                    .createdAt(LocalDateTime.now())
                    .published(false)
                    .attemptCount(0)
                    .build();
            when(outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                    .thenReturn(List.of(event));
            doThrow(new RuntimeException("broker unavailable"))
                    .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

            publisher.publishPendingEvents();

            assertThat(event.isPublished()).isFalse();
            assertThat(event.getLastError()).isEqualTo("broker unavailable");
            assertThat(event.getAttemptCount()).isEqualTo(1);
            verify(outboxEventRepository).save(event);
        }

        @Test
        @DisplayName("dead-letters event when max attempts exceeded")
        void deadLettersEventWhenMaxAttemptsExceeded() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .id("event-poison")
                    .eventType("PAYMENT_RESERVE_REPLY")
                    .payload("{}")
                    .exchange("ex")
                    .routingKey("rk")
                    .createdAt(LocalDateTime.now())
                    .published(false)
                    .attemptCount(5)
                    .lastError("broker unavailable")
                    .build();
            when(outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                    .thenReturn(List.of(event));

            publisher.publishPendingEvents();

            verifyNoInteractions(rabbitTemplate);
            verify(outboxEventRepository).save(entityCaptor.capture());
            OutboxEventEntity saved = entityCaptor.getValue();
            assertThat(saved.isDeadLettered()).isTrue();
            assertThat(saved.getDeadLetteredAt()).isNotNull();
            assertThat(saved.isPublished()).isFalse();
        }

        @Test
        @DisplayName("does not dead-letter event below max attempts")
        void doesNotDeadLetterBelowMaxAttempts() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .id("event-retry")
                    .eventType("PAYMENT_RESERVE_REPLY")
                    .payload("{}")
                    .exchange("ex")
                    .routingKey("rk")
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
