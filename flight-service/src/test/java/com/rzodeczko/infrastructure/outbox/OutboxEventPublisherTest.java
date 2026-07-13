package com.rzodeczko.infrastructure.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        publisher = new OutboxEventPublisher(outboxEventRepository, rabbitTemplate, 5);
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
        void shouldPublishUnpublishedEventAndMarkSuccess() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .id("evt-1")
                    .eventType("FLIGHT_RESERVE_REPLY")
                    .payload("{}")
                    .exchange("x.saga.replies")
                    .routingKey("flight.reply")
                    .createdAt(LocalDateTime.now())
                    .published(false)
                    .build();
            when(outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                    .thenReturn(List.of(event));

            publisher.publishPendingEvents();

            verify(rabbitTemplate).send(org.mockito.ArgumentMatchers.eq("x.saga.replies"), org.mockito.ArgumentMatchers.eq("flight.reply"), any(Message.class));
            verify(outboxEventRepository).save(entityCaptor.capture());
            OutboxEventEntity saved = entityCaptor.getValue();
            assertThat(saved.isPublished()).isTrue();
            assertThat(saved.getPublishedAt()).isNotNull();
            assertThat(saved.getAttemptCount()).isEqualTo(1);
        }

        @Test
        void shouldMarkFailureWhenRabbitTemplateThrows() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .id("evt-2")
                    .eventType("FLIGHT_CANCEL_REPLY")
                    .payload("{}")
                    .exchange("x.saga.replies")
                    .routingKey("flight.reply")
                    .createdAt(LocalDateTime.now())
                    .published(false)
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
        void shouldPublishMultipleEventsIndependently() {
            OutboxEventEntity ok = OutboxEventEntity.builder()
                    .id("evt-ok")
                    .eventType("A")
                    .payload("{}")
                    .exchange("ex")
                    .routingKey("rk")
                    .createdAt(LocalDateTime.now())
                    .published(false)
                    .build();
            OutboxEventEntity failing = OutboxEventEntity.builder()
                    .id("evt-fail")
                    .eventType("B")
                    .payload("{}")
                    .exchange("ex")
                    .routingKey("rk")
                    .createdAt(LocalDateTime.now())
                    .published(false)
                    .build();
            when(outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                    .thenReturn(List.of(ok, failing));
            doThrow(new RuntimeException("boom"))
                    .when(rabbitTemplate).send(org.mockito.ArgumentMatchers.eq("ex"), org.mockito.ArgumentMatchers.eq("rk"), any(Message.class));

            publisher.publishPendingEvents();

            verify(outboxEventRepository, times(2)).save(any());
        }

        @Test
        void shouldDeadLetterEventWhenMaxAttemptsExceeded() {
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .id("evt-poison")
                    .eventType("FLIGHT_RESERVE_REPLY")
                    .payload("{}")
                    .exchange("x.saga.replies")
                    .routingKey("flight.reply")
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
                    .eventType("FLIGHT_RESERVE_REPLY")
                    .payload("{}")
                    .exchange("x.saga.replies")
                    .routingKey("flight.reply")
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
