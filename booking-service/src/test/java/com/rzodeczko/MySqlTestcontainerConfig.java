package com.rzodeczko;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestcontainerConfig {
    private static final MySQLContainer MYSQL =
            new MySQLContainer(DockerImageName.parse("mysql:9.6.0"));

    static {
        MYSQL.start();

    }

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return MYSQL;
    }
}
