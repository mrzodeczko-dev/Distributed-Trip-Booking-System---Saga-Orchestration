package com.rzodeczko;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({RabbitMqTestcontainersConfig.class, MySqlTestcontainerConfig.class})
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {

    @BeforeEach
    @Sql(statements = {"delete from outbox_event", "delete from saga_step", "delete fromłó saga_instance"})
    void cleanDatabase() {
    }
}
