package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.IntegrationTestBase;
import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStatus;
import com.rzodeczko.domain.model.saga.SagaStepName;
import com.rzodeczko.domain.model.saga.SagaStepStatus;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessage;
import com.rzodeczko.infrastructure.persistence.adapter.SagaInstanceRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test verifying SagaReplyListener consumes
 * real RabbitMQ messages and updates saga state in MySQL.
 */
class  SagaReplyListenerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SagaInstanceRepositoryAdapter repository;

    @Test
    void shouldProcessReplyMessageFromRabbitMq() {
        // Create saga with FLIGHT already reserved (waiting for HOTEL reply)
        SagaInstance saga = SagaInstance.start("Listener Test", "Prague", new BigDecimal("2500.00"));
        saga.markReserved(SagaStepName.FLIGHT);
        repository.save(saga);
        UUID sagaId = saga.getId();

        // Send HOTEL RESERVE SUCCESS reply via RabbitMQ
        SagaReplyMessage reply = new SagaReplyMessage(
                sagaId, "HOTEL", "RESERVE", "SUCCESS", null);
        rabbitTemplate.convertAndSend("x.saga.replies", "saga.reply", reply);

        // Listener should consume the message and mark HOTEL as RESERVED
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            SagaInstance updated = repository.findById(sagaId).orElseThrow();
            assertThat(updated.getStep(SagaStepName.HOTEL).getStatus())
                    .isEqualTo(SagaStepStatus.RESERVED);
        });
    }

    @Test
    void shouldTriggerCompensationOnFailureReply() {
        SagaInstance saga = SagaInstance.start("Comp Listener", "Dubai", new BigDecimal("8000.00"));
        saga.markReserved(SagaStepName.FLIGHT);
        repository.save(saga);
        UUID sagaId = saga.getId();

        // HOTEL fails
        SagaReplyMessage reply = new SagaReplyMessage(
                sagaId, "HOTEL", "RESERVE", "FAILURE", "Hotel full");
        rabbitTemplate.convertAndSend("x.saga.replies", "saga.reply", reply);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            SagaInstance updated = repository.findById(sagaId).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
            assertThat(updated.getStep(SagaStepName.HOTEL).getReason()).isEqualTo("Hotel full");
        });
    }

    @Test
    void shouldIgnoreReplyForTerminalSaga() {
        SagaInstance saga = SagaInstance.start("Terminal Test", "Cairo", new BigDecimal("1000.00"));
        saga.markReserved(SagaStepName.FLIGHT);
        saga.markReserved(SagaStepName.HOTEL);
        saga.markReserved(SagaStepName.PAYMENT);
        saga.complete();
        repository.save(saga);
        UUID sagaId = saga.getId();

        // Send a late reply — should be ignored
        SagaReplyMessage reply = new SagaReplyMessage(
                sagaId, "FLIGHT", "RESERVE", "SUCCESS", null);
        rabbitTemplate.convertAndSend("x.saga.replies", "saga.reply", reply);

        // Brief wait, then verify status didn't change
        await().during(2, TimeUnit.SECONDS).atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            SagaInstance updated = repository.findById(sagaId).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        });
    }
}
