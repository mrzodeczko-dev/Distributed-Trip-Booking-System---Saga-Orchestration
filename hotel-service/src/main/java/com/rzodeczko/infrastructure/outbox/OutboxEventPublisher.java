package com.rzodeczko.infrastructure.outbox;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class OutboxEventPublisher {
    private final JpaOutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final int maxAttempts;

    public OutboxEventPublisher(
            JpaOutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.outbox.max-attempts:5}") int maxAttempts
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${app.rabbitmq.outbox.poll-interval-ms:1000}")
    @SchedulerLock(name = "hotel_outbox_publisher", lockAtMostFor = "30s", lockAtLeastFor = "500ms")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> unpublished = outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc();
        if (unpublished.isEmpty()) {
            return;
        }
        log.debug("[OUTBOX] Found {} unpublished event(s)", unpublished.size());
        for (OutboxEventEntity event : unpublished) {
            publishSingle(event);
        }
    }

    private void publishSingle(OutboxEventEntity event) {
        if (event.exceededMaxAttempts(maxAttempts)) {
            event.deadLetter();
            outboxEventRepository.save(event);
            log.error("[OUTBOX] Dead-lettered type={}, id={}, attempts={}, lastError={}",
                    event.getEventType(), event.getId(), event.getAttemptCount(), event.getLastError());
            return;
        }

        try {
            Message message = MessageBuilder
                    .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();
            rabbitTemplate.send(event.getExchange(), event.getRoutingKey(), message);

            event.publishSuccess();
            outboxEventRepository.save(event);
            log.info("[OUTBOX] Published type={}, id={}, attempts={}",
                    event.getEventType(), event.getId(), event.getAttemptCount());
        } catch (Exception e) {
            event.publishFailure(e.getMessage());
            outboxEventRepository.save(event);
            log.error("[OUTBOX] Publish failed type={}, id={}, attempt={}, error={}",
                    event.getEventType(), event.getId(), event.getAttemptCount(), e.getMessage());
        }
    }
}
