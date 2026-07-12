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
    private JpaProcessedMessageRepository repository;

    @Captor
    private ArgumentCaptor<ProcessedMessageEntity> entityCaptor;

    private ProcessedMessageStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProcessedMessageStoreAdapter(repository);
    }

    @Nested
    @DisplayName("existsByMessageKey")
    class ExistsByMessageKey {

        @Test
        void shouldDelegateToRepository() {
            when(repository.existsByMessageKey("saga:RESERVE")).thenReturn(true);

            boolean result = adapter.existsByMessageKey("saga:RESERVE");

            assertThat(result).isTrue();
            verify(repository).existsByMessageKey("saga:RESERVE");
        }

        @Test
        void shouldReturnFalseWhenNotFound() {
            when(repository.existsByMessageKey("saga:CANCEL")).thenReturn(false);

            boolean result = adapter.existsByMessageKey("saga:CANCEL");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("markAsProcessed")
    class MarkAsProcessed {

        @Test
        void shouldSaveEntityWithGivenMessageKey() {
            adapter.markAsProcessed("saga:RESERVE");

            verify(repository).save(entityCaptor.capture());
            ProcessedMessageEntity saved = entityCaptor.getValue();
            assertThat(saved.getMessageKey()).isEqualTo("saga:RESERVE");
            assertThat(saved.getProcessedAt()).isNotNull();
        }
    }
}
