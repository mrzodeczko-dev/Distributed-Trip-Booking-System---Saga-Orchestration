package com.rzodeczko.infrastructure.persistence;

import com.rzodeczko.IntegrationTestBase;
import com.rzodeczko.domain.model.saga.*;
import com.rzodeczko.infrastructure.persistence.adapter.SagaInstanceRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SagaInstanceRepositoryAdapterIntegrationTest extends IntegrationTestBase {

    @Autowired
    private SagaInstanceRepositoryAdapter repository;

    @Test
    void shouldSaveNewSagaAndFindById() {
        SagaInstance saga = SagaInstance.start("Test User", "Berlin", new BigDecimal("1500.00"));
        repository.save(saga);

        Optional<SagaInstance> found = repository.findById(saga.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCustomerName()).isEqualTo("Test User");
        assertThat(found.get().getDestination()).isEqualTo("Berlin");
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(found.get().getStatus()).isEqualTo(SagaStatus.IN_PROGRESS);
        assertThat(found.get().getSteps()).hasSize(3);
    }

    @Test
    void shouldUpdateExistingSaga() {
        SagaInstance saga = SagaInstance.start("Update Test", "Vienna", new BigDecimal("2000.00"));
        repository.save(saga);

        // Modify and save again
        saga.markReserved(SagaStepName.FLIGHT);
        repository.save(saga);

        SagaInstance updated = repository.findById(saga.getId()).orElseThrow();
        assertThat(updated.getStep(SagaStepName.FLIGHT).getStatus()).isEqualTo(SagaStepStatus.RESERVED);
    }

    @Test
    void shouldFindByIdForUpdate() {
        SagaInstance saga = SagaInstance.start("Lock Test", "London", new BigDecimal("3000.00"));
        repository.save(saga);

        Optional<SagaInstance> locked = repository.findByIdForUpdate(saga.getId());

        assertThat(locked).isPresent();
        assertThat(locked.get().getId()).isEqualTo(saga.getId());
    }

    @Test
    void shouldFindAll() {
        SagaInstance saga = SagaInstance.start("FindAll Test", "Madrid", new BigDecimal("500.00"));
        repository.save(saga);

        List<SagaInstance> all = repository.findAll();

        assertThat(all).isNotEmpty();
        assertThat(all.stream().anyMatch(s -> s.getId().equals(saga.getId()))).isTrue();
    }

    @Test
    void shouldReturnEmptyForNonExistentId() {
        Optional<SagaInstance> found = repository.findById(java.util.UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    void shouldPersistCompleteSagaLifecycle() {
        SagaInstance saga = SagaInstance.start("Lifecycle", "Oslo", new BigDecimal("4000.00"));
        repository.save(saga);

        // Reserve all steps
        saga.markReserved(SagaStepName.FLIGHT);
        repository.save(saga);

        saga.markReserved(SagaStepName.HOTEL);
        repository.save(saga);

        saga.markReserved(SagaStepName.PAYMENT);
        saga.complete();
        repository.save(saga);

        SagaInstance completed = repository.findById(saga.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(completed.getSteps()).allMatch(SagaStep::isReserved);
    }

    @Test
    void shouldPersistFailureAndCompensation() {
        SagaInstance saga = SagaInstance.start("Fail Test", "Sydney", new BigDecimal("6000.00"));
        repository.save(saga);

        saga.markReserved(SagaStepName.FLIGHT);
        saga.failAndStartCompensation(SagaStepName.HOTEL, "No rooms");
        repository.save(saga);

        SagaInstance compensating = repository.findById(saga.getId()).orElseThrow();
        assertThat(compensating.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
        assertThat(compensating.getStep(SagaStepName.HOTEL).getStatus()).isEqualTo(SagaStepStatus.FAILED);
        assertThat(compensating.getStep(SagaStepName.HOTEL).getReason()).isEqualTo("No rooms");

        // Compensate flight
        saga.markCompensated(SagaStepName.FLIGHT);
        saga.cancel();
        repository.save(saga);

        SagaInstance cancelled = repository.findById(saga.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(SagaStatus.CANCELLED);
        assertThat(cancelled.getStep(SagaStepName.FLIGHT).getStatus()).isEqualTo(SagaStepStatus.COMPENSATED);
    }
}
