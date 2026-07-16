package com.rzodeczko.common.idempotency;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JpaProcessedMessageStore implements ProcessedMessageStore {

    private final ProcessedMessageRepository repository;

    @Override
    public boolean existsByMessageKey(String messageKey) {
        return repository.existsByMessageKey(messageKey);
    }

    @Override
    public void markProcessed(String messageKey) {
        repository.save(ProcessedMessageEntity.of(messageKey));
    }
}
