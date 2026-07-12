package com.rzodeczko.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaOutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {
    List<OutboxEventEntity> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
