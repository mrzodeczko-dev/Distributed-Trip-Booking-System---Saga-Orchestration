package com.rzodeczko.infrastructure.outbox;

import com.rzodeczko.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OutboxEventPublisherIntegrationTest extends IntegrationTestBase {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Test
    void shouldPublishPendingEventsAndMarkAsPublished() {
        OutboxEvent event = OutboxEvent.builder()
                .eventType("TEST_EVENT")
                .payload("{\"test\": true}")
                .exchange("x.saga.commands")
                .routingKey("flight.command")
                .createdAt(LocalDateTime.now())
                .published(false)
                .build();
        outboxEventRepository.save(event);

        // Wait for scheduler to pick it up
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
            assertThat(updated.isPublished()).isTrue();
            assertThat(updated.getPublishedAt()).isNotNull();
            assertThat(updated.getAttemptCount()).isGreaterThan(0);
        });
    }

    @Test
    void shouldDoNothingWhenNoUnpublishedEvents() {
        // Mark all existing events as published
        outboxEventRepository.findAll().forEach(e -> {
            e.publishSuccess();
            outboxEventRepository.save(e);
        });

        long countBefore = outboxEventRepository.count();

        // Manually invoke — should not fail
        outboxEventPublisher.publishPendingEvents();

        assertThat(outboxEventRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void shouldHandlePublishFailureGracefully() {
        // Create event with invalid exchange that doesn't exist — returns callback fires
        // but since we use mandatory=true, the message gets returned
        OutboxEvent event = OutboxEvent.builder()
                .eventType("FAIL_TEST")
                .payload("{\"fail\": true}")
                .exchange("non.existent.exchange.that.does.not.exist")
                .routingKey("bad.key")
                .createdAt(LocalDateTime.now())
                .published(false)
                .build();
        outboxEventRepository.save(event);

        // Wait for scheduler to pick it up (direct call may be skipped by ShedLock)
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
            assertThat(updated.getAttemptCount()).isGreaterThan(0);
        });
    }

    @Test
    void shouldRecordPublishFailureOnEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setEventType("TEST");
        event.setPayload("{}");
        event.setExchange("x");
        event.setRoutingKey("k");
        event.setCreatedAt(LocalDateTime.now());
        event.setPublished(false);
        event.setAttemptCount(0);

        event.publishFailure("Connection refused");

        assertThat(event.isPublished()).isFalse();
        assertThat(event.getLastError()).isEqualTo("Connection refused");
        assertThat(event.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void shouldRecordPublishSuccessOnEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setEventType("TEST");
        event.setPayload("{}");
        event.setExchange("x");
        event.setRoutingKey("k");
        event.setCreatedAt(LocalDateTime.now());
        event.setPublished(false);
        event.setAttemptCount(0);

        event.publishSuccess();

        assertThat(event.isPublished()).isTrue();
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttemptCount()).isEqualTo(1);
    }
}
