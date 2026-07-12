package com.rzodeczko.infrastructure.idempotency;

import com.rzodeczko.application.port.out.ProcessedMessageStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessedMessageStoreAdapter implements ProcessedMessageStore {

    private final JpaProcessedMessageRepository processedMessageRepository;

    @Override
    public boolean existsByMessageKey(String messageKey) {
        return processedMessageRepository.existsByMessageKey(messageKey);
    }

    @Override
    public void markProcessed(String messageKey) {
        processedMessageRepository.save(ProcessedMessageEntity.of(messageKey));
    }
}
