package com.rzodeczko;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {

    @BeforeEach
    @Sql(statements = {"truncate table outbox_event", "TRUNCATE TABLE saga_step", "TRUNCATE TABLE saga_instance"})
    void cleanDatabase() {
    }
}
