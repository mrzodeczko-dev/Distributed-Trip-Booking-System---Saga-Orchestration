package com.rzodeczko;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class RabbitMqTestcontainersConfig {


    private static final RabbitMQContainer RABBIT =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

    static {
        RABBIT.start();
    }

    // Exchanges
    @Bean
    public DirectExchange sagaCommandsExchange() {
        return new DirectExchange("x.saga.commands", true, false);
    }

    @Bean
    public DirectExchange sagaRepliesExchange() {
        return new DirectExchange("x.saga.replies", true, false);
    }

    // Command queues
    @Bean
    public Queue flightCommandQueue() {
        return new Queue("q.flight.commands", true);
    }

    @Bean
    public Queue hotelCommandQueue() {
        return new Queue("q.hotel.commands", true);
    }

    @Bean
    public Queue paymentCommandQueue() {
        return new Queue("q.payment.commands", true);
    }

    // Bindings for commands
    @Bean
    public Binding flightCommandBinding(
            Queue flightCommandQueue,
                DirectExchange sagaCommandsExchange) {
        return BindingBuilder.bind(flightCommandQueue)
                .to(sagaCommandsExchange)
                .with("flight.command");
    }

    @Bean
    public Binding hotelCommandBinding(
            Queue hotelCommandQueue,
            DirectExchange sagaCommandsExchange) {
        return BindingBuilder.bind(hotelCommandQueue)
                .to(sagaCommandsExchange)
                .with("hotel.command");
    }

    @Bean
    public Binding paymentCommandBinding(
            Queue paymentCommandQueue,
            DirectExchange sagaCommandsExchange) {
        return BindingBuilder.bind(paymentCommandQueue)
                .to(sagaCommandsExchange)
                .with("payment.command");
    }

    // Reply queue
    @Bean
    public Queue sagaReplyQueue() {
        return new Queue("q.saga.replies", true);
    }

    @Bean
    public Binding sagaReplyBinding(
            Queue sagaReplyQueue,
            DirectExchange sagaRepliesExchange) {
        return BindingBuilder.bind(sagaReplyQueue)
                .to(sagaRepliesExchange)
                .with("saga.reply");
    }


    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitMQContainer() {
        return RABBIT;
    }
}
