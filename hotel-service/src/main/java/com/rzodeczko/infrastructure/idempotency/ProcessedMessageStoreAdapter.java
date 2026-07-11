package com.rzodeczko.infrastructure.idempotency;

import com.rzodeczko.application.port.out.ProcessedMessageStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessedMessageStoreAdapter implements ProcessedMessageStore {
    private final JpaProcessedMessageRepository repository;

    @Override
    public boolean existsByMessageKey(String messageKey) {
        return repository.existsByMessageKey(messageKey);
    }

    @Override
    public void markAsProcessed(String messageKey) {
        repository.save(ProcessedMessageEntity.of(messageKey));
    }
}
