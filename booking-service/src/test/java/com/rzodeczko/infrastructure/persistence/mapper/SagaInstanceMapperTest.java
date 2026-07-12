package com.rzodeczko.infrastructure.persistence.mapper;

import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStatus;
import com.rzodeczko.domain.model.saga.SagaStep;
import com.rzodeczko.domain.model.saga.SagaStepName;
import com.rzodeczko.domain.model.saga.SagaStepStatus;
import com.rzodeczko.infrastructure.persistence.entity.SagaInstanceEntity;
import com.rzodeczko.infrastructure.persistence.entity.SagaStepEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SagaInstanceMapper")
class SagaInstanceMapperTest {

    private final SagaInstanceMapper mapper = new SagaInstanceMapper();

    private static final String CUSTOMER = "Jan Kowalski";
    private static final String DESTINATION = "Mars";
    private static final BigDecimal AMOUNT = new BigDecimal("2500.00");

    @Nested
    @DisplayName("toNewEntity()")
    class ToNewEntity {

        @Test
        void shouldMapAllFieldsAndSteps() {
            SagaInstance saga = SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);

            SagaInstanceEntity entity = mapper.toNewEntity(saga);

            assertThat(entity.getId()).isEqualTo(saga.getId());
            assertThat(entity.getCustomerName()).isEqualTo(CUSTOMER);
            assertThat(entity.getDestination()).isEqualTo(DESTINATION);
            assertThat(entity.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(entity.getStatus()).isEqualTo(SagaStatus.IN_PROGRESS);
            assertThat(entity.getCreatedAt()).isEqualTo(saga.getCreatedAt());
            assertThat(entity.getUpdatedAt()).isEqualTo(saga.getUpdatedAt());

            assertThat(entity.getSteps()).hasSize(3);
            assertThat(entity.getSteps())
                    .extracting(SagaStepEntity::getName)
                    .containsExactlyInAnyOrder(SagaStepName.FLIGHT, SagaStepName.HOTEL, SagaStepName.PAYMENT);
            assertThat(entity.getSteps())
                    .allSatisfy(step -> assertThat(step.getStatus()).isEqualTo(SagaStepStatus.PENDING));
        }

        @Test
        void shouldLinkStepsBackToParentEntity() {
            SagaInstance saga = SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);

            SagaInstanceEntity entity = mapper.toNewEntity(saga);

            assertThat(entity.getSteps()).allSatisfy(step -> assertThat(step.getSaga()).isSameAs(entity));
        }
    }

    @Nested
    @DisplayName("updateEntity()")
    class UpdateEntity {

        @Test
        void shouldUpdateStatusAndStepsFromDomain() {
            SagaInstance saga = SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);
            SagaInstanceEntity entity = mapper.toNewEntity(saga);

            saga.markReserved(SagaStepName.FLIGHT);
            saga.failAndStartCompensation(SagaStepName.HOTEL, "No cabins");

            mapper.updateEntity(entity, saga);

            assertThat(entity.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
            assertThat(entity.getUpdatedAt()).isEqualTo(saga.getUpdatedAt());

            SagaStepEntity flightStep = entity.getSteps().stream()
                    .filter(s -> s.getName() == SagaStepName.FLIGHT)
                    .findFirst().orElseThrow();
            assertThat(flightStep.getStatus()).isEqualTo(SagaStepStatus.RESERVED);

            SagaStepEntity hotelStep = entity.getSteps().stream()
                    .filter(s -> s.getName() == SagaStepName.HOTEL)
                    .findFirst().orElseThrow();
            assertThat(hotelStep.getStatus()).isEqualTo(SagaStepStatus.FAILED);
            assertThat(hotelStep.getReason()).isEqualTo("No cabins");
        }
    }

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {

        @Test
        void shouldMapEntityBackToDomainSortedByStepOrdinal() {
            UUID id = UUID.randomUUID();
            Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
            Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");

            SagaInstanceEntity entity = SagaInstanceEntity.builder()
                    .id(id)
                    .customerName(CUSTOMER)
                    .destination(DESTINATION)
                    .amount(AMOUNT)
                    .status(SagaStatus.IN_PROGRESS)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .steps(List.of(
                            SagaStepEntity.builder()
                                    .name(SagaStepName.PAYMENT)
                                    .status(SagaStepStatus.PENDING)
                                    .build(),
                            SagaStepEntity.builder()
                                    .name(SagaStepName.FLIGHT)
                                    .status(SagaStepStatus.RESERVED)
                                    .build(),
                            SagaStepEntity.builder()
                                    .name(SagaStepName.HOTEL)
                                    .status(SagaStepStatus.FAILED)
                                    .reason("No cabins")
                                    .build()
                    ))
                    .build();

            SagaInstance saga = mapper.toDomain(entity);

            assertThat(saga.getId()).isEqualTo(id);
            assertThat(saga.getCustomerName()).isEqualTo(CUSTOMER);
            assertThat(saga.getDestination()).isEqualTo(DESTINATION);
            assertThat(saga.getAmount()).isEqualByComparingTo(AMOUNT);
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.IN_PROGRESS);
            assertThat(saga.getCreatedAt()).isEqualTo(createdAt);
            assertThat(saga.getUpdatedAt()).isEqualTo(updatedAt);

            List<SagaStep> steps = saga.getSteps();
            assertThat(steps).hasSize(3);
            assertThat(steps.get(0).getName()).isEqualTo(SagaStepName.FLIGHT);
            assertThat(steps.get(1).getName()).isEqualTo(SagaStepName.HOTEL);
            assertThat(steps.get(2).getName()).isEqualTo(SagaStepName.PAYMENT);

            assertThat(saga.getStep(SagaStepName.HOTEL).getReason()).isEqualTo("No cabins");
        }
    }
}
