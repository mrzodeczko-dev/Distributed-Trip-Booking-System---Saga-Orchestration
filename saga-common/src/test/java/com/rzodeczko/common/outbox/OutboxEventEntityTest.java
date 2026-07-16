package com.rzodeczko.common.outbox;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventEntityTest {

    private OutboxEventEntity newEvent() {
        return OutboxEventEntity.builder()
                .eventType("flight.command")
                .payload("{\"sagaId\":\"abc\"}")
                .exchange("x.saga.commands")
                .routingKey("flight.command")
                .createdAt(LocalDateTime.now())
                .published(false)
                .build();
    }

    @Test
    void publishSuccessShouldMarkPublishedAndIncrementAttemptCount() {
        OutboxEventEntity event = newEvent();

        event.publishSuccess();

        assertThat(event.isPublished()).isTrue();
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void publishFailureShouldNotMarkPublishedAndRecordError() {
        OutboxEventEntity event = newEvent();

        event.publishFailure("connection refused");

        assertThat(event.isPublished()).isFalse();
        assertThat(event.getLastError()).isEqualTo("connection refused");
        assertThat(event.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void multipleFailuresShouldAccumulateAttemptCount() {
        OutboxEventEntity event = newEvent();

        event.publishFailure("fail 1");
        event.publishFailure("fail 2");
        event.publishFailure("fail 3");

        assertThat(event.getAttemptCount()).isEqualTo(3);
        assertThat(event.getLastError()).isEqualTo("fail 3");
    }

    @Test
    void deadLetterShouldSetFlagAndTimestamp() {
        OutboxEventEntity event = newEvent();

        event.deadLetter();

        assertThat(event.isDeadLettered()).isTrue();
        assertThat(event.getDeadLetteredAt()).isNotNull();
    }

    @Test
    void exceededMaxAttemptsShouldBeTrueOnceCountReachesLimit() {
        OutboxEventEntity event = newEvent();
        event.publishFailure("a");
        event.publishFailure("b");
        event.publishFailure("c");

        assertThat(event.exceededMaxAttempts(3)).isTrue();
        assertThat(event.exceededMaxAttempts(4)).isFalse();
    }

    @Test
    void exceededMaxAttemptsShouldBeFalseAtStart() {
        assertThat(newEvent().exceededMaxAttempts(1)).isFalse();
    }
}
