package com.rzodeczko.common.idempotency;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autokonfiguracja rejestru przetworzonych wiadomości (idempotencja).
 * Encja i repozytorium są znajdowane przez domyślny Spring Boot scan
 * (aplikacja musi być w com.rzodeczko lub wyżej).
 */
@AutoConfiguration
@ConditionalOnClass(JpaRepository.class)
public class IdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProcessedMessageStore processedMessageStore(ProcessedMessageRepository repository) {
        return new JpaProcessedMessageStore(repository);
    }
}
