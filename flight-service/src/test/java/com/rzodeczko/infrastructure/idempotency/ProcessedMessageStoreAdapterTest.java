package com.rzodeczko.infrastructure.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedMessageStoreAdapterTest {

    @Mock
    private JpaProcessedMessageRepository jpaProcessedMessageRepository;

    @Captor
    private ArgumentCaptor<ProcessedMessageEntity> entityCaptor;

    private ProcessedMessageStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProcessedMessageStoreAdapter(jpaProcessedMessageRepository);
    }

    @Nested
    @DisplayName("existsByMessageKey")
    class ExistsByMessageKey {

        @Test
        void shouldDelegateToRepository() {
            when(jpaProcessedMessageRepository.existsByMessageKey("saga-1:RESERVE")).thenReturn(true);

            boolean result = adapter.existsByMessageKey("saga-1:RESERVE");

            assertThat(result).isTrue();
            verify(jpaProcessedMessageRepository).existsByMessageKey("saga-1:RESERVE");
        }

        @Test
        void shouldReturnFalseWhenNotProcessed() {
            when(jpaProcessedMessageRepository.existsByMessageKey("saga-2:CANCEL")).thenReturn(false);

            boolean result = adapter.existsByMessageKey("saga-2:CANCEL");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("markProcessed")
    class MarkProcessed {

        @Test
        void shouldPersistEntityWithGivenMessageKey() {
            adapter.markProcessed("saga-1:RESERVE");

            verify(jpaProcessedMessageRepository).save(entityCaptor.capture());
            assertThat(entityCaptor.getValue().getMessageKey()).isEqualTo("saga-1:RESERVE");
            assertThat(entityCaptor.getValue().getProcessedAt()).isNotNull();
        }
    }
}
