package com.rzodeczko.infrastructure.persistence;

import com.rzodeczko.domain.model.ReservationStatus;
import com.rzodeczko.domain.model.SeatReservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SeatReservationMapper")
class SeatReservationMapperTest {

    private final SeatReservationMapper mapper = new SeatReservationMapper();

    private static final UUID ID = UUID.randomUUID();
    private static final UUID SAGA_ID = UUID.randomUUID();
    private static final String CUSTOMER = "Jan Kowalski";
    private static final String DESTINATION = "Mars";
    private static final Instant CREATED_AT = Instant.now();

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        void shouldMapDomainFieldsToEntity() {
            SeatReservation reservation = SeatReservation.restore(
                    ID, SAGA_ID, CUSTOMER, DESTINATION, ReservationStatus.RESERVED, CREATED_AT
            );

            SeatReservationEntity entity = mapper.toEntity(reservation);

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
        void shouldMapEntityFieldsToDomain() {
            SeatReservationEntity entity = SeatReservationEntity.builder()
                    .id(ID)
                    .sagaId(SAGA_ID)
                    .customerName(CUSTOMER)
                    .destination(DESTINATION)
                    .status(ReservationStatus.CANCELLED)
                    .createdAt(CREATED_AT)
                    .build();

            SeatReservation domain = mapper.toDomain(entity);

            assertThat(domain.getId()).isEqualTo(ID);
            assertThat(domain.getSagaId()).isEqualTo(SAGA_ID);
            assertThat(domain.getCustomerName()).isEqualTo(CUSTOMER);
            assertThat(domain.getDestination()).isEqualTo(DESTINATION);
            assertThat(domain.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(domain.getCreatedAt()).isEqualTo(CREATED_AT);
        }

        @Test
        void shouldRoundTripThroughEntityAndBackToDomain() {
            SeatReservation original = SeatReservation.restore(
                    ID, SAGA_ID, CUSTOMER, DESTINATION, ReservationStatus.RESERVED, CREATED_AT
            );

            SeatReservation roundTripped = mapper.toDomain(mapper.toEntity(original));

            assertThat(roundTripped.getId()).isEqualTo(original.getId());
            assertThat(roundTripped.getSagaId()).isEqualTo(original.getSagaId());
            assertThat(roundTripped.getStatus()).isEqualTo(original.getStatus());
        }
    }
}
