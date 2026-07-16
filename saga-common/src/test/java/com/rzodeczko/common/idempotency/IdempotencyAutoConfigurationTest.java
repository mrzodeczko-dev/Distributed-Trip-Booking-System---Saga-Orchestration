package com.rzodeczko.common.idempotency;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IdempotencyAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdempotencyAutoConfiguration.class))
            .withUserConfiguration(DependenciesConfig.class);

    @Test
    void shouldRegisterProcessedMessageStoreBean() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(ProcessedMessageStore.class);
            assertThat(context.getBean(ProcessedMessageStore.class))
                    .isInstanceOf(JpaProcessedMessageStore.class);
        });
    }

    @Test
    void shouldBackOffWhenUserProvidesTheirOwnStore() {
        runner.withUserConfiguration(CustomStoreConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ProcessedMessageStore.class);
                    assertThat(context.getBean(ProcessedMessageStore.class))
                            .isNotInstanceOf(JpaProcessedMessageStore.class);
                });
    }

    @Configuration
    static class DependenciesConfig {
        @Bean
        ProcessedMessageRepository processedMessageRepository() {
            return mock(ProcessedMessageRepository.class);
        }
    }

    @Configuration
    static class CustomStoreConfig {
        @Bean
        ProcessedMessageStore customStore() {
            return new ProcessedMessageStore() {
                @Override public boolean existsByMessageKey(String messageKey) { return false; }
                @Override public void markProcessed(String messageKey) {}
            };
        }
    }
}
