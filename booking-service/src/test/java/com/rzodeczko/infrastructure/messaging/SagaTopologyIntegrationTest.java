package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.IntegrationTestBase;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that RabbitMQ topology (exchanges, queues, bindings)
 * and RabbitTemplate are correctly configured by Spring context.
 */
class SagaTopologyIntegrationTest extends IntegrationTestBase {

    @Autowired
    private Declarables sagaTopology;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SagaTopologyProperties sagaTopologyProperties;

    @Test
    void shouldCreateAllTopologyDeclarables() {
        // 3 exchanges + 2 queues + 2 bindings = 7
        assertThat(sagaTopology.getDeclarables()).hasSize(7);
    }

    @Test
    void shouldConfigureRabbitTemplateWithJsonConverter() {
        assertThat(rabbitTemplate.getMessageConverter())
                .isInstanceOf(org.springframework.amqp.support.converter.JacksonJsonMessageConverter.class);
    }

    @Test
    void shouldLoadTopologyProperties() {
        assertThat(sagaTopologyProperties.commandsExchange()).isEqualTo("x.saga.commands");
        assertThat(sagaTopologyProperties.repliesExchange()).isEqualTo("x.saga.replies");
        assertThat(sagaTopologyProperties.replyQueue()).isEqualTo("q.booking-service.replies");
        assertThat(sagaTopologyProperties.flightCommandRoutingKey()).isEqualTo("flight.command");
        assertThat(sagaTopologyProperties.hotelCommandRoutingKey()).isEqualTo("hotel.command");
        assertThat(sagaTopologyProperties.paymentCommandRoutingKey()).isEqualTo("payment.command");
    }
}
