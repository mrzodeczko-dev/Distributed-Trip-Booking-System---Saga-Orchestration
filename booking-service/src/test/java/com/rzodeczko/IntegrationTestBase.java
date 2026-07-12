package com.rzodeczko;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static final MySQLContainer mysql = new MySQLContainer("mysql:9.6.0")
            .withDatabaseName("booking_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

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
