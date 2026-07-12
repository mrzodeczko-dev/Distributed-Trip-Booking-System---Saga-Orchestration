package com.rzodeczko.infrastructure.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventEntityTest {

    @Test
    @DisplayName("publishSuccess marks as published and increments attempt")
    void publishSuccess() {
        var entity = OutboxEventEntity.builder()
                .eventType("REPLY")
                .payload("{}")
                .exchange("ex")
                .routingKey("rk")
                .createdAt(LocalDateTime.now())
                .published(false)
                .build();

        entity.publishSuccess();

        assertThat(entity.isPublished()).isTrue();
        assertThat(entity.getPublishedAt()).isNotNull();
        assertThat(entity.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("publishFailure keeps unpublished, stores error and increments attempt")
    void publishFailure() {
        var entity = OutboxEventEntity.builder()
                .eventType("REPLY")
                .payload("{}")
                .exchange("ex")
                .routingKey("rk")
                .createdAt(LocalDateTime.now())
                .published(false)
                .build();

        entity.publishFailure("connection refused");

        assertThat(entity.isPublished()).isFalse();
        assertThat(entity.getLastError()).isEqualTo("connection refused");
        assertThat(entity.getAttemptCount()).isEqualTo(1);
    }
}
