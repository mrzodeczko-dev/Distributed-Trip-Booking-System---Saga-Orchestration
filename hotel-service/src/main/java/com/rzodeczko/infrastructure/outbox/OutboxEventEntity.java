package com.rzodeczko.infrastructure.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private String exchange;

    @Column(nullable = false)
    private String routingKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean published;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    @Builder.Default
    private boolean deadLettered = false;

    private LocalDateTime deadLetteredAt;

    public void publishSuccess() {
        this.published = true;
        this.publishedAt = LocalDateTime.now();
        this.attemptCount++;
    }

    public void publishFailure(String errorMessage) {
        this.published = false;
        this.lastError = errorMessage;
        this.attemptCount++;
    }

    public void deadLetter() {
        this.deadLettered = true;
        this.deadLetteredAt = LocalDateTime.now();
    }

    public boolean exceededMaxAttempts(int maxAttempts) {
        return this.attemptCount >= maxAttempts;
    }
}
