package com.rzodeczko.infrastructure.persistence.entity;

import com.rzodeczko.domain.model.saga.SagaStepName;
import com.rzodeczko.domain.model.saga.SagaStepStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "saga_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SagaStepEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saga_id", nullable = false)
    private SagaInstanceEntity saga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SagaStepName name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SagaStepStatus status;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
