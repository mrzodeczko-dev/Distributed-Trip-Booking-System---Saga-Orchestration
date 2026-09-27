package com.rzodeczko.common.outbox;

/**
 * The broker did not accept an outbox message: it answered with a NACK, returned it as unroutable
 * (mandatory flag) or did not confirm it in time. The event stays unpublished and is retried.
 */
public class OutboxPublishException extends RuntimeException {
    public OutboxPublishException(String message) {
        super(message);
    }
}
