package com.rzodeczko.infrastructure.persistence;

import com.rzodeczko.IntegrationTestBase;
import com.rzodeczko.domain.model.saga.*;
import com.rzodeczko.infrastructure.persistence.adapter.SagaInstanceRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
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
    @Transactional
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

        com.rzodeczko.application.dto.PageResult<SagaInstance> all =
                repository.findAll(new com.rzodeczko.application.dto.PageQuery(0, 100));

        assertThat(all.content()).isNotEmpty();
        assertThat(all.content().stream().anyMatch(s -> s.getId().equals(saga.getId()))).isTrue();
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

    @Test
    void shouldFindOnlyActiveSagasIdleLongerThanCutoffOldestFirst() {
        Instant now = Instant.now();
        SagaInstance oldest = sagaWithStatus(SagaStatus.IN_PROGRESS, now.minus(Duration.ofHours(3)));
        SagaInstance olderCompensating = sagaWithStatus(SagaStatus.COMPENSATING, now.minus(Duration.ofHours(2)));
        SagaInstance oldButCompleted = sagaWithStatus(SagaStatus.COMPLETED, now.minus(Duration.ofHours(3)));
        SagaInstance freshInProgress = sagaWithStatus(SagaStatus.IN_PROGRESS, now);
        List.of(oldest, olderCompensating, oldButCompleted, freshInProgress).forEach(repository::save);

        List<UUID> stuckIds = repository.findStuck(
                        EnumSet.of(SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING),
                        now.minus(Duration.ofMinutes(30)),
                        200)
                .stream()
                .map(SagaInstance::getId)
                .toList();

        assertThat(stuckIds)
                .contains(oldest.getId(), olderCompensating.getId())
                .doesNotContain(oldButCompleted.getId(), freshInProgress.getId());
        assertThat(stuckIds.indexOf(oldest.getId())).isLessThan(stuckIds.indexOf(olderCompensating.getId()));
    }

    @Test
    void shouldReturnStuckSagaWithSteps() {
        Instant old = Instant.now().minus(Duration.ofHours(1));
        SagaInstance saga = sagaWithStatus(SagaStatus.IN_PROGRESS, old);
        repository.save(saga);

        SagaInstance found = repository.findStuck(EnumSet.of(SagaStatus.IN_PROGRESS), Instant.now(), 200)
                .stream()
                .filter(s -> s.getId().equals(saga.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(found.getSteps()).hasSize(3);
    }

    @Test
    void shouldReturnEmptyListWhenNothingMatches() {
        assertThat(repository.findStuck(EnumSet.of(SagaStatus.IN_PROGRESS), Instant.EPOCH, 10)).isEmpty();
    }

    private static SagaInstance sagaWithStatus(SagaStatus status, Instant updatedAt) {
        List<SagaStep> steps = List.of(
                new SagaStep(SagaStepName.FLIGHT),
                new SagaStep(SagaStepName.HOTEL),
                new SagaStep(SagaStepName.PAYMENT));
        return SagaInstance.restore(UUID.randomUUID(), "Stuck Test", "Rome", new BigDecimal("100.00"),
                status, new java.util.ArrayList<>(steps), updatedAt, updatedAt);
    }
}
