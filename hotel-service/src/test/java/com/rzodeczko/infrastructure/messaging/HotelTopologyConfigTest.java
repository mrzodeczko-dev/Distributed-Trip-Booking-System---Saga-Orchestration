package com.rzodeczko.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class HotelTopologyConfigTest {

    private final ParticipantTopologyProperties props = new ParticipantTopologyProperties(
            "x.saga.commands", "x.saga.replies", "x.saga.dlx",
            "q.hotel-service.commands", "q.hotel-service.commands.dlq",
            "hotel.command", "hotel.dlq", "saga.reply");

    private final HotelTopologyConfig config = new HotelTopologyConfig(props);

    @Test
    void shouldDeclareAllExchangesQueuesAndBindings() {
        Declarables declarables = config.hotelTopology();

        long exchanges = declarables.getDeclarables().stream().filter(d -> d instanceof DirectExchange).count();
        long queues = declarables.getDeclarables().stream().filter(d -> d instanceof Queue).count();
        long bindings = declarables.getDeclarables().stream().filter(d -> d instanceof Binding).count();

        assertThat(exchanges).isEqualTo(3);
        assertThat(queues).isEqualTo(2);
        assertThat(bindings).isEqualTo(2);
    }

    @Test
    void shouldNameExchangesFromProperties() {
        Declarables declarables = config.hotelTopology();

        assertThat(declarables.getDeclarables().stream()
                .filter(d -> d instanceof DirectExchange)
                .map(d -> ((DirectExchange) d).getName()))
                .containsExactlyInAnyOrder("x.saga.commands", "x.saga.replies", "x.saga.dlx");
    }

    @Test
    void shouldNameCommandQueueAndDlqFromProperties() {
        Declarables declarables = config.hotelTopology();

        assertThat(declarables.getDeclarables().stream()
                .filter(d -> d instanceof Queue)
                .map(d -> ((Queue) d).getName()))
                .containsExactlyInAnyOrder("q.hotel-service.commands", "q.hotel-service.commands.dlq");
    }
}
