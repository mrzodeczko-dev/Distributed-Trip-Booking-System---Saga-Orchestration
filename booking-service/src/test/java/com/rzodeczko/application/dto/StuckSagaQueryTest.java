package com.rzodeczko.application.dto;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class StuckSagaQueryTest {

    @Test
    void shouldAcceptValidValues() {
        StuckSagaQuery query = new StuckSagaQuery(Duration.ofMinutes(10), 50);

        assertThat(query.idleFor()).isEqualTo(Duration.ofMinutes(10));
        assertThat(query.limit()).isEqualTo(50);
    }

    @Test
    void shouldAcceptLimitBoundaries() {
        assertThat(new StuckSagaQuery(Duration.ofSeconds(1), 1).limit()).isEqualTo(1);
        assertThat(new StuckSagaQuery(Duration.ofSeconds(1), StuckSagaQuery.MAX_LIMIT).limit())
                .isEqualTo(StuckSagaQuery.MAX_LIMIT);
    }

    @Test
    void shouldRejectNullZeroOrNegativeIdleTime() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StuckSagaQuery(null, 10));
        assertThatIllegalArgumentException().isThrownBy(() -> new StuckSagaQuery(Duration.ZERO, 10));
        assertThatIllegalArgumentException().isThrownBy(() -> new StuckSagaQuery(Duration.ofMinutes(-1), 10));
    }

    @Test
    void shouldRejectLimitOutOfRange() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StuckSagaQuery(Duration.ofMinutes(1), 0));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new StuckSagaQuery(Duration.ofMinutes(1), StuckSagaQuery.MAX_LIMIT + 1));
    }
}
