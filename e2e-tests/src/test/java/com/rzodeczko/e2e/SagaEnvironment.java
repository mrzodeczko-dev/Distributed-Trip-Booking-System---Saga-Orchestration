package com.rzodeczko.e2e;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

/**
 * Manages the full saga stack via docker-compose for E2E testing.
 * Singleton started once per JVM
 */
public final class SagaEnvironment {

    private static final Logger LOG = LoggerFactory.getLogger(SagaEnvironment.class);

    private static final String BOOKING_SERVICE = "booking-service";
    private static final int BOOKING_PORT = 8080;

    private static ComposeContainer compose;

    private SagaEnvironment() {
    }

    public static synchronized void start() {
        if (compose != null) {
            return;
        }
        LOG.info("Starting saga E2E environment via docker-compose…");

        compose = new ComposeContainer(new File("docker-compose.yml"))
                .withExposedService(BOOKING_SERVICE, BOOKING_PORT,
                        Wait.forHttp("/actuator/health")
                                .forStatusCode(200)
                                .withStartupTimeout(Duration.ofMinutes(4)))
                .withExposedService("flight-service", BOOKING_PORT,
                        Wait.forHttp("/actuator/health")
                                .forStatusCode(200)
                                .withStartupTimeout(Duration.ofMinutes(4)))
                .withExposedService("hotel-service", BOOKING_PORT,
                        Wait.forHttp("/actuator/health")
                                .forStatusCode(200)
                                .withStartupTimeout(Duration.ofMinutes(4)))
                .withExposedService("payment-service", BOOKING_PORT,
                        Wait.forHttp("/actuator/health")
                                .forStatusCode(200)
                                .withStartupTimeout(Duration.ofMinutes(4)));

        compose.start();
        LOG.info("Saga E2E environment is up. Booking-service at {}:{}",
                getBookingHost(), getBookingPort());
    }

    public static String getBookingHost() {
        return compose.getServiceHost(BOOKING_SERVICE, BOOKING_PORT);
    }

    public static int getBookingPort() {
        return compose.getServicePort(BOOKING_SERVICE, BOOKING_PORT);
    }

    public static String bookingBaseUrl() {
        return "http://" + getBookingHost() + ":" + getBookingPort();
    }
}
