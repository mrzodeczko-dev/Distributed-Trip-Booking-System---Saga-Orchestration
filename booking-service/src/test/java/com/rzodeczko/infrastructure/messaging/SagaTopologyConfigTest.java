package com.rzodeczko.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class SagaTopologyConfigTest {

    private final SagaTopologyProperties props = new SagaTopologyProperties(
            "x.saga.commands", "x.saga.replies", "x.saga.dlx",
            "q.booking-service.replies", "q.booking-service.replies.dlq",
            "saga.reply", "reply.dlq",
            "flight.command", "hotel.command", "payment.command");

    private final SagaTopologyConfig config = new SagaTopologyConfig(props);

    @Test
    void shouldDeclareThreeExchangesTwoQueuesAndTwoBindings() {
        Declarables declarables = config.sagaTopology();

        long exchanges = declarables.getDeclarables().stream().filter(d -> d instanceof DirectExchange).count();
        long queues = declarables.getDeclarables().stream().filter(d -> d instanceof Queue).count();
        long bindings = declarables.getDeclarables().stream().filter(d -> d instanceof Binding).count();

        assertThat(exchanges).isEqualTo(3);
        assertThat(queues).isEqualTo(2);
        assertThat(bindings).isEqualTo(2);
    }

    @Test
    void shouldExposeExchangeNamesFromProperties() {
        Declarables declarables = config.sagaTopology();

        assertThat(declarables.getDeclarables().stream()
                .filter(d -> d instanceof DirectExchange)
                .map(d -> ((DirectExchange) d).getName()))
                .containsExactlyInAnyOrder("x.saga.commands", "x.saga.replies", "x.saga.dlx");
    }

    @Test
    void shouldExposeReplyQueueAndDlqFromProperties() {
        Declarables declarables = config.sagaTopology();

        assertThat(declarables.getDeclarables().stream()
                .filter(d -> d instanceof Queue)
                .map(d -> ((Queue) d).getName()))
                .containsExactlyInAnyOrder("q.booking-service.replies", "q.booking-service.replies.dlq");
    }
}
