package com.rzodeczko;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    static final MySQLContainer mysql;
    static final RabbitMQContainer rabbit;

    static {
        mysql = new MySQLContainer("mysql:9.6.0")
                .withDatabaseName("booking_test")
                .withUsername("test")
                .withPassword("test");
        mysql.start();

        rabbit = new RabbitMQContainer("rabbitmq:3.13-management");
        rabbit.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.rabbitmq.addresses", rabbit::getAmqpUrl);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("spring.rabbitmq.virtual-host", () -> "/");

        registry.add("app.rabbitmq.topology.commands-exchange", () -> "x.saga.commands");
        registry.add("app.rabbitmq.topology.replies-exchange", () -> "x.saga.replies");
        registry.add("app.rabbitmq.topology.dlx-exchange", () -> "x.saga.dlx");
        registry.add("app.rabbitmq.topology.reply-queue", () -> "q.booking-service.replies");
        registry.add("app.rabbitmq.topology.reply-dlq", () -> "q.booking-service.replies.dlq");
        registry.add("app.rabbitmq.topology.reply-routing-key", () -> "saga.reply");
        registry.add("app.rabbitmq.topology.reply-dlq-routing-key", () -> "reply.dlq");
        registry.add("app.rabbitmq.topology.flight-command-routing-key", () -> "flight.command");
        registry.add("app.rabbitmq.topology.hotel-command-routing-key", () -> "hotel.command");
        registry.add("app.rabbitmq.topology.payment-command-routing-key", () -> "payment.command");
        registry.add("app.rabbitmq.outbox.poll-interval-ms", () -> "500");
    }
}
