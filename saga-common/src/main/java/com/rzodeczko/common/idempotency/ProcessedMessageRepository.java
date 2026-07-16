package com.rzodeczko.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessageEntity, Long> {
    boolean existsByMessageKey(String messageKey);
}
