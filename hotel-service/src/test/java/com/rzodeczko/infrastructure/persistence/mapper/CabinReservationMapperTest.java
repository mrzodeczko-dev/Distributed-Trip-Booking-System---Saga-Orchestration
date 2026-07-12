package com.rzodeczko.infrastructure.persistence.mapper;

import com.rzodeczko.domain.model.CabinReservation;
import com.rzodeczko.domain.model.ReservationStatus;
import com.rzodeczko.infrastructure.persistence.entity.CabinReservationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CabinReservationMapperTest {

    private final CabinReservationMapper mapper = new CabinReservationMapper();

    private static final UUID ID = UUID.randomUUID();
    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final String CUSTOMER = "Jan Kowalski";
    private static final String DESTINATION = "Venus";
    private static final Instant CREATED_AT = Instant.parse("2024-05-01T12:00:00Z");

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        void shouldMapDomainToEntity() {
            CabinReservation reservation = CabinReservation.restore(
                    ID, SAGA_ID, CUSTOMER, DESTINATION, ReservationStatus.RESERVED, CREATED_AT
            );

            CabinReservationEntity entity = mapper.toEntity(reservation);

            assertThat(entity.getId()).isEqualTo(ID);
            assertThat(entity.getSagaId()).isEqualTo(SAGA_ID);
            assertThat(entity.getCustomerName()).isEqualTo(CUSTOMER);
            assertThat(entity.getDestination()).isEqualTo(DESTINATION);
            assertThat(entity.getStatus()).isEqualTo(ReservationStatus.RESERVED);
            assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        void shouldMapEntityToDomain() {
            CabinReservationEntity entity = CabinReservationEntity.builder()
                    .id(ID)
                    .sagaId(SAGA_ID)
                    .customerName(CUSTOMER)
                    .destination(DESTINATION)
                    .status(ReservationStatus.CANCELLED)
                    .createdAt(CREATED_AT)
                    .build();

            CabinReservation reservation = mapper.toDomain(entity);

            assertThat(reservation.getId()).isEqualTo(ID);
            assertThat(reservation.getSagaId()).isEqualTo(SAGA_ID);
            assertThat(reservation.getCustomerName()).isEqualTo(CUSTOMER);
            assertThat(reservation.getDestination()).isEqualTo(DESTINATION);
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(reservation.getCreatedAt()).isEqualTo(CREATED_AT);
        }
    }

    @Nested
    @DisplayName("round trip")
    class RoundTrip {

        @Test
        void shouldPreserveDataThroughEntityAndBackToDomain() {
            CabinReservation original = CabinReservation.restore(
                    ID, SAGA_ID, CUSTOMER, DESTINATION, ReservationStatus.RESERVED, CREATED_AT
            );

            CabinReservation roundTripped = mapper.toDomain(mapper.toEntity(original));

            assertThat(roundTripped.getId()).isEqualTo(original.getId());
            assertThat(roundTripped.getSagaId()).isEqualTo(original.getSagaId());
            assertThat(roundTripped.getCustomerName()).isEqualTo(original.getCustomerName());
            assertThat(roundTripped.getDestination()).isEqualTo(original.getDestination());
            assertThat(roundTripped.getStatus()).isEqualTo(original.getStatus());
            assertThat(roundTripped.getCreatedAt()).isEqualTo(original.getCreatedAt());
        }
    }
}
