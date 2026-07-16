package com.rzodeczko.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTopologyConfigTest {

    private final ParticipantTopologyProperties props = new ParticipantTopologyProperties(
            "x.saga.commands", "x.saga.replies", "x.saga.dlx",
            "q.payment-service.commands", "q.payment-service.commands.dlq",
            "payment.command", "payment.dlq", "saga.reply");

    private final PaymentTopologyConfig config = new PaymentTopologyConfig(props);

    @Test
    void shouldDeclareAllExchangesQueuesAndBindings() {
        Declarables declarables = config.paymentTopology();

        long exchanges = declarables.getDeclarables().stream().filter(d -> d instanceof DirectExchange).count();
        long queues = declarables.getDeclarables().stream().filter(d -> d instanceof Queue).count();
        long bindings = declarables.getDeclarables().stream().filter(d -> d instanceof Binding).count();

        assertThat(exchanges).isEqualTo(3);
        assertThat(queues).isEqualTo(2);
        assertThat(bindings).isEqualTo(2);
    }

    @Test
    void shouldNameExchangesFromProperties() {
        Declarables declarables = config.paymentTopology();

        assertThat(declarables.getDeclarables().stream()
                .filter(d -> d instanceof DirectExchange)
                .map(d -> ((DirectExchange) d).getName()))
                .containsExactlyInAnyOrder("x.saga.commands", "x.saga.replies", "x.saga.dlx");
    }

    @Test
    void shouldNameCommandQueueAndDlqFromProperties() {
        Declarables declarables = config.paymentTopology();

        assertThat(declarables.getDeclarables().stream()
                .filter(d -> d instanceof Queue)
                .map(d -> ((Queue) d).getName()))
                .containsExactlyInAnyOrder("q.payment-service.commands", "q.payment-service.commands.dlq");
    }
}
