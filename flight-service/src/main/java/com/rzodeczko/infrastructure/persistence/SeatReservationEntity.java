package com.rzodeczko.infrastructure.persistence;

import com.rzodeczko.domain.model.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "seat_reservations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seat_reservation_saga",
                columnNames = "saga_id"

        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatReservationEntity {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID sagaId;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReservationStatus status;

    @Column(nullable = false)
    private Instant createdAt;
}
