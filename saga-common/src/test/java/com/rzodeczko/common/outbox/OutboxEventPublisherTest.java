package com.rzodeczko.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxEventPublisher(repository, rabbitTemplate, new ObjectMapper(), 5);
    }

    private OutboxEventEntity event() {
        return OutboxEventEntity.builder()
                .id("evt-1")
                .eventType("flight.command")
                .payload("{\"sagaId\":\"saga-1\"}")
                .exchange("x.saga.commands")
                .routingKey("flight.command")
                .createdAt(LocalDateTime.now())
                .published(false)
                .build();
    }

    @Test
    void shouldDoNothingWhenNoUnpublishedEvents() {
        when(repository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                .thenReturn(List.of());

        publisher.publishPendingEvents();

        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldPublishAndMarkAsSent() {
        OutboxEventEntity e = event();
        when(repository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(e));

        publisher.publishPendingEvents();

        verify(rabbitTemplate).send(eq("x.saga.commands"), eq("flight.command"), any(Message.class));
        verify(repository).save(e);
        assertThat(e.isPublished()).isTrue();
        assertThat(e.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void shouldMarkFailureWhenRabbitThrows() {
        OutboxEventEntity e = event();
        when(repository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(e));
        doThrow(new AmqpException("broker down"))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        publisher.publishPendingEvents();

        assertThat(e.isPublished()).isFalse();
        assertThat(e.getLastError()).contains("broker down");
        assertThat(e.getAttemptCount()).isEqualTo(1);
        verify(repository).save(e);
    }

    @Test
    void shouldDeadLetterEventWhenExceededMaxAttempts() {
        OutboxEventEntity e = event();
        // Push attempt count to 5 (which is the maxAttempts)
        e.publishFailure("fail 1");
        e.publishFailure("fail 2");
        e.publishFailure("fail 3");
        e.publishFailure("fail 4");
        e.publishFailure("fail 5");

        when(repository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(e));

        publisher.publishPendingEvents();

        assertThat(e.isDeadLettered()).isTrue();
        assertThat(e.getDeadLetteredAt()).isNotNull();
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        verify(repository).save(e);
    }

    @Test
    void shouldProcessMultipleEventsInSingleTick() {
        OutboxEventEntity a = event();
        OutboxEventEntity b = event();
        when(repository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(a, b));

        publisher.publishPendingEvents();

        verify(rabbitTemplate, times(2)).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    void shouldHandleMalformedPayloadWithoutBlowingUp() {
        OutboxEventEntity e = OutboxEventEntity.builder()
                .id("evt-x")
                .eventType("bad")
                .payload("not-a-json")
                .exchange("x")
                .routingKey("r")
                .createdAt(LocalDateTime.now())
                .published(false)
                .build();
        when(repository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(e));

        publisher.publishPendingEvents();

        // Publisher recovers from malformed sagaId — still tries to publish
        verify(rabbitTemplate).send(anyString(), anyString(), any(Message.class));
        assertThat(e.isPublished()).isTrue();
    }
}
