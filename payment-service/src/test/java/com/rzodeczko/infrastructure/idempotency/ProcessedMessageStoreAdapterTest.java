package com.rzodeczko.infrastructure.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessedMessageStoreAdapter")
class ProcessedMessageStoreAdapterTest {

    @Mock
    private JpaProcessedMessageRepository processedMessageRepository;

    private ProcessedMessageStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProcessedMessageStoreAdapter(processedMessageRepository);
    }

    @Nested
    @DisplayName("existsByMessageKey")
    class ExistsByMessageKey {

        @Test
        @DisplayName("delegates to the JPA repository")
        void delegatesToRepository() {
            when(processedMessageRepository.existsByMessageKey("key-1")).thenReturn(true);

            boolean result = adapter.existsByMessageKey("key-1");

            assertThat(result).isTrue();
            verify(processedMessageRepository).existsByMessageKey("key-1");
        }
    }

    @Nested
    @DisplayName("markProcessed")
    class MarkProcessed {

        @Test
        @DisplayName("saves a new ProcessedMessageEntity built from the message key")
        void savesProcessedEntity() {
            adapter.markProcessed("key-1");

            ArgumentCaptor<ProcessedMessageEntity> captor = ArgumentCaptor.forClass(ProcessedMessageEntity.class);
            verify(processedMessageRepository).save(captor.capture());
            assertThat(captor.getValue().getMessageKey()).isEqualTo("key-1");
            assertThat(captor.getValue().getProcessedAt()).isNotNull();
        }
    }
}
