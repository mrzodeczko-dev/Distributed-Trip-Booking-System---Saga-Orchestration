package com.rzodeczko.infrastructure.idempotency;

import com.rzodeczko.application.port.out.ProcessedMessageStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessedMessageStoreAdapter implements ProcessedMessageStore {

    private final JpaProcessedMessageRepository jpaProcessedMessageRepository;

    @Override
    public boolean existsByMessageKey(String messageKey) {
        return jpaProcessedMessageRepository.existsByMessageKey(messageKey);
    }

    @Override
    public void markProcessed(String messageKey) {
        jpaProcessedMessageRepository.save(ProcessedMessageEntity.of(messageKey));
    }
}
