package com.rzodeczko.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Autokonfiguracja mechanizmu Outbox:
 * - encja OutboxEventEntity i repozytorium OutboxEventRepository są znajdowane
 *   przez domyślny Spring Boot scan (pod warunkiem, że aplikacja jest w pakiecie
 *   com.rzodeczko lub wyżej),
 * - beany OutboxEventService i OutboxEventPublisher tworzone tutaj.
 *
 * Wymagane property w serwisie konsumującym:
 *   app.rabbitmq.outbox.lock-name
 * Opcjonalne (mają wartości domyślne):
 *   app.rabbitmq.outbox.poll-interval-ms (1000)
 *   app.rabbitmq.outbox.max-attempts (5)
 *   app.rabbitmq.outbox.enabled (true)
 */
@AutoConfiguration
@ConditionalOnClass({RabbitTemplate.class, ObjectMapper.class})
@ConditionalOnProperty(prefix = "app.rabbitmq.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "30s")
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxEventService outboxEventService(
            OutboxEventRepository repository,
            ObjectMapper objectMapper
    ) {
        return new OutboxEventService(repository, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxEventPublisher outboxEventPublisher(
            OutboxEventRepository repository,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${app.rabbitmq.outbox.max-attempts:5}") int maxAttempts
    ) {
        return new OutboxEventPublisher(repository, rabbitTemplate, objectMapper, maxAttempts);
    }
}
