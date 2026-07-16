package com.rzodeczko.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OutboxAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
            .withUserConfiguration(DependenciesConfig.class);

    @Test
    void shouldRegisterOutboxBeansWhenEnabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(OutboxEventService.class);
            assertThat(context).hasSingleBean(OutboxEventPublisher.class);
        });
    }

    @Test
    void shouldNotRegisterOutboxBeansWhenExplicitlyDisabled() {
        runner.withPropertyValues("app.rabbitmq.outbox.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OutboxEventService.class);
                    assertThat(context).doesNotHaveBean(OutboxEventPublisher.class);
                });
    }

    @Test
    void shouldRespectCustomMaxAttemptsProperty() {
        runner.withPropertyValues("app.rabbitmq.outbox.max-attempts=42")
                .run(context -> assertThat(context).hasSingleBean(OutboxEventPublisher.class));
    }

    @Configuration
    static class DependenciesConfig {
        @Bean
        RabbitTemplate rabbitTemplate() { return mock(RabbitTemplate.class); }

        @Bean
        ObjectMapper objectMapper() { return new ObjectMapper(); }

        @Bean
        OutboxEventRepository outboxEventRepository() { return mock(OutboxEventRepository.class); }
    }
}
