package com.rzodeczko.common.idempotency;

/**
 * Port wyjściowy: rejestr przetworzonych komend (idempotencja at-least-once).
 */
public interface ProcessedMessageStore {
    boolean existsByMessageKey(String messageKey);
    void markProcessed(String messageKey);
}
