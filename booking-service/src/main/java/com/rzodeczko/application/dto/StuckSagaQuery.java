package com.rzodeczko.application.dto;

import java.time.Duration;

/**
 * Query for sagas that are still active (IN_PROGRESS / COMPENSATING) but have not changed for a while.
 * Such sagas usually wait for a reply that was dead-lettered or lost.
 */
public record StuckSagaQuery(Duration idleFor, int limit) {
    public static final int MAX_LIMIT = 200;

    public StuckSagaQuery {
        if (idleFor == null || idleFor.isZero() || idleFor.isNegative()) {
            throw new IllegalArgumentException("idleFor must be a positive duration");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("Limit must be between 1 and " + MAX_LIMIT);
        }
    }
}
