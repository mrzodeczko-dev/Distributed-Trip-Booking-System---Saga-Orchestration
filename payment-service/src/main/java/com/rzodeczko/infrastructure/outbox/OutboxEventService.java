package com.rzodeczko.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final JpaOutboxEventRepository outboxEventRepository;
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
            log.debug("[OUTBOX] Saved event type={}, id={}", eventType, saved.getId());
            return saved;
        } catch (JsonProcessingException e) {
            throw new OutboxSerializationException("Cannot serialize payload for eventType: " + eventType, e);
        }
    }
}

