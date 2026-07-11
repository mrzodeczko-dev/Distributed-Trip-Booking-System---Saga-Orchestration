package com.rzodeczko.infrastructure.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProcessedMessageRepository extends JpaRepository<ProcessedMessageEntity, Long> {
    boolean existsByMessageKey(String messageKey);
}
