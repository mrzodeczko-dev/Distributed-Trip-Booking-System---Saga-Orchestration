package com.rzodeczko.common.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Outbox relay: publishes pending outbox events to RabbitMQ.
 * <p>
 * An event is marked as published only after the broker has <b>confirmed</b> it (publisher confirm ACK) and has
 * <b>not returned</b> it as unroutable (mandatory flag). A NACK, a return or a missing confirm within
 * {@code confirmTimeout} counts as a failed attempt, so the event is retried and eventually dead-lettered in the
 * outbox instead of being silently lost. This requires {@code spring.rabbitmq.publisher-confirm-type=correlated}
 * (and {@code publisher-returns=true} + {@code template.mandatory=true} to detect unroutable messages); without
 * confirms the relay falls back to fire-and-forget and logs a warning.
 */
@Slf4j
public class OutboxEventPublisher {
    public static final Duration DEFAULT_CONFIRM_TIMEOUT = Duration.ofSeconds(10);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;
    private final Duration confirmTimeout;
    private boolean fireAndForgetWarningLogged;

    public OutboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            int maxAttempts
    ) {
        this(outboxEventRepository, rabbitTemplate, objectMapper, maxAttempts, DEFAULT_CONFIRM_TIMEOUT);
    }

    public OutboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            int maxAttempts,
            Duration confirmTimeout
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.maxAttempts = maxAttempts;
        this.confirmTimeout = confirmTimeout;
    }

    @Scheduled(fixedDelayString = "${app.rabbitmq.outbox.poll-interval-ms:1000}")
    @SchedulerLock(name = "${app.rabbitmq.outbox.lock-name}", lockAtMostFor = "30s", lockAtLeastFor = "500ms")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> unpublished = outboxEventRepository
                .findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc();
        if (unpublished.isEmpty()) {
            return;
        }

        log.debug("[OUTBOX] Found {} unpublished event(s)", unpublished.size());
        for (OutboxEventEntity event : unpublished) {
            if (!publishSingle(event)) {
                // Broker did not answer in time - it is most likely unavailable. Stop this batch instead of
                // waiting confirmTimeout for every remaining event; they are picked up by the next poll.
                break;
            }
        }
    }

    /**
     * @return false if the batch should stop (no confirm within the timeout or the thread was interrupted)
     */
    private boolean publishSingle(OutboxEventEntity event) {
        String sagaId = extractSagaId(event.getPayload());
        if (sagaId != null) {
            MDC.put("sagaId", sagaId);
        }
        try {
            if (event.exceededMaxAttempts(maxAttempts)) {
                event.deadLetter();
                outboxEventRepository.save(event);
                log.error(
                        "[OUTBOX] Dead-lettered type={}, id={}, attempts={}, lastError={}",
                        event.getEventType(),
                        event.getId(),
                        event.getAttemptCount(),
                        event.getLastError()
                );
                return true;
            }

            Message message = MessageBuilder
                    .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setHeader("sagaId", sagaId)
                    .build();
            send(event, message);
            event.publishSuccess();
            outboxEventRepository.save(event);
            log.info(
                    "[OUTBOX] Published type={}, id={}, attempts={}",
                    event.getEventType(),
                    event.getId(),
                    event.getAttemptCount()
            );
            return true;
        } catch (TimeoutException e) {
            recordFailure(event, "No publisher confirm within " + confirmTimeout.toMillis() + " ms");
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordFailure(event, "Interrupted while waiting for publisher confirm");
            return false;
        } catch (Exception e) {
            recordFailure(event, e.getMessage());
            return true;
        } finally {
            MDC.remove("sagaId");
        }
    }

    /**
     * Sends the message and, when publisher confirms are enabled, waits until the broker has confirmed it.
     *
     * @throws OutboxPublishException the broker NACKed the message or returned it as unroutable
     * @throws TimeoutException       no confirm within {@code confirmTimeout}
     */
    private void send(OutboxEventEntity event, Message message)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (!confirmsEnabled()) {
            rabbitTemplate.send(event.getExchange(), event.getRoutingKey(), message);
            return;
        }

        CorrelationData correlation = new CorrelationData(String.valueOf(event.getId()));
        rabbitTemplate.send(event.getExchange(), event.getRoutingKey(), message, correlation);
        CorrelationData.Confirm confirm = correlation.getFuture()
                .get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);

        if (!confirm.ack()) {
            throw new OutboxPublishException("Broker NACK: " + confirm.reason());
        }
        // With mandatory=true an unroutable message is still ACKed by the broker, but the return is
        // attached to the CorrelationData before the confirm future completes.
        ReturnedMessage returned = correlation.getReturned();
        if (returned != null) {
            throw new OutboxPublishException("Unroutable message returned by broker: exchange=%s, routingKey=%s, replyCode=%d, replyText=%s"
                    .formatted(returned.getExchange(), returned.getRoutingKey(),
                            returned.getReplyCode(), returned.getReplyText()));
        }
    }

    private boolean confirmsEnabled() {
        boolean enabled = rabbitTemplate.getConnectionFactory().isPublisherConfirms();
        if (!enabled && !fireAndForgetWarningLogged) {
            fireAndForgetWarningLogged = true;
            log.warn("[OUTBOX] Publisher confirms are disabled - events are marked as published without broker "
                    + "confirmation. Set spring.rabbitmq.publisher-confirm-type=correlated.");
        }
        return enabled;
    }

    private void recordFailure(OutboxEventEntity event, String error) {
        event.publishFailure(error);
        outboxEventRepository.save(event);
        log.error(
                "[OUTBOX] Publish failed type={}, id={}, attempt={}, error={}",
                event.getEventType(),
                event.getId(),
                event.getAttemptCount(),
                error
        );
    }

    private String extractSagaId(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode sagaIdNode = node.get("sagaId");
            return sagaIdNode != null ? sagaIdNode.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
