package com.rzodeczko;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({RabbitMqTestcontainersConfig.class, MySqlTestcontainerConfig.class})
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {
}
