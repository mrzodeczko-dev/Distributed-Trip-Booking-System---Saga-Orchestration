package com.rzodeczko.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Zapisuje event do Outboxa. Propagation.MANDATORY wymusza istniejącą transakcję —
 * event powstaje atomowo razem ze zmianą stanu domeny.
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxEventService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEventEntity save(String eventType, Object payload, String exchange, String routingKey) {
        try {
            String serializedPayload = objectMapper.writeValueAsString(payload);
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .eventType(eventType)
                    .payload(serializedPayload)
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .createdAt(LocalDateTime.now())
                    .published(false)
                    .build();
            OutboxEventEntity saved = outboxEventRepository.save(event);
            log.debug("[OUTBOX] Saved event type={}, id={}", saved.getEventType(), saved.getId());
            return saved;
        } catch (JsonProcessingException e) {
            throw new OutboxSerializationException("Cannot serialize payload for eventType: " + eventType, e);
        }
    }
}
