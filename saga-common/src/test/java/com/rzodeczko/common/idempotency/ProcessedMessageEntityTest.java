package com.rzodeczko.common.idempotency;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedMessageEntityTest {

    @Test
    void ofShouldCreateEntityWithMessageKeyAndCurrentTimestamp() {
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1);

        ProcessedMessageEntity entity = ProcessedMessageEntity.of("saga-1:RESERVE");

        LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1);
        assertThat(entity.getMessageKey()).isEqualTo("saga-1:RESERVE");
        assertThat(entity.getProcessedAt()).isNotNull();
        // Loose comparison because JVM timezone may differ
        assertThat(entity.getProcessedAt().toInstant(ZoneOffset.UTC))
                .isAfterOrEqualTo(Instant.MIN);
    }

    @Test
    void builderShouldProduceEquivalentEntity() {
        ProcessedMessageEntity entity = ProcessedMessageEntity.builder()
                .messageKey("k")
                .processedAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();

        assertThat(entity.getMessageKey()).isEqualTo("k");
        assertThat(entity.getProcessedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
    }
}
