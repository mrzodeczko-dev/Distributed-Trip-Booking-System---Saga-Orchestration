package com.rzodeczko.common.idempotency;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "processed_messages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_processed_message_key",
                columnNames = "message_key"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    public static ProcessedMessageEntity of(String messageKey) {
        return ProcessedMessageEntity.builder()
                .messageKey(messageKey)
                .processedAt(LocalDateTime.now())
                .build();
    }
}
