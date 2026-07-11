package com.rzodeczko.application.port.out;

public interface ProcessedMessageStore {
    boolean existsByMessageKey(String messageKey);
    void markAsProcessed(String messageKey);
}
