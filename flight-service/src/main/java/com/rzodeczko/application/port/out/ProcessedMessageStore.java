package com.rzodeczko.application.port.out;

/**
 * Port wyjsciowy: rejestr przetworzonych komend (idempotencja at-least-once).
 */
public interface ProcessedMessageStore {
    boolean existsByMessageKey(String messageKey);
    void markProcessed(String messageKey);
}
