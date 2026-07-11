package com.rzodeczko.infrastructure.idempotency;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "processed_messages",
        uniqueConstraints = @UniqueConstraint(name = "uk_processed_message_key", columnNames = "messageKey")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedMessageEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
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
