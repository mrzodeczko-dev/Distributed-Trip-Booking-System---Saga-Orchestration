package com.rzodeczko.common.idempotency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaProcessedMessageStoreTest {

    @Mock
    private ProcessedMessageRepository repository;

    @InjectMocks
    private JpaProcessedMessageStore store;

    @Test
    void existsByMessageKeyShouldDelegateToRepository() {
        when(repository.existsByMessageKey("saga-1:RESERVE")).thenReturn(true);

        assertThat(store.existsByMessageKey("saga-1:RESERVE")).isTrue();
        verify(repository).existsByMessageKey("saga-1:RESERVE");
    }

    @Test
    void existsByMessageKeyShouldReturnFalseWhenNotPresent() {
        when(repository.existsByMessageKey("unknown")).thenReturn(false);

        assertThat(store.existsByMessageKey("unknown")).isFalse();
    }

    @Test
    void markProcessedShouldSaveNewEntity() {
        store.markProcessed("saga-2:CANCEL");

        ArgumentCaptor<ProcessedMessageEntity> captor = ArgumentCaptor.forClass(ProcessedMessageEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getMessageKey()).isEqualTo("saga-2:CANCEL");
        assertThat(captor.getValue().getProcessedAt()).isNotNull();
    }
}
