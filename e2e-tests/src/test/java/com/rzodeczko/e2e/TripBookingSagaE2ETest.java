package com.rzodeczko.e2e;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end tests that exercise the full saga flow:
 * booking-service -> flight-service -> hotel-service -> payment-service
 * communicating through RabbitMQ with real MySQL databases
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TripBookingSagaE2ETest {

    private static final Logger LOG = LoggerFactory.getLogger(TripBookingSagaE2ETest.class);

    @BeforeAll
    static void startEnvironment() {
        SagaEnvironment.start();
    }

    private Response awaitTerminalState(String sagaId, int timeoutSeconds) {
        AtomicReference<String> lastStatus = new AtomicReference<>("unknown");

        await().atMost(timeoutSeconds, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Response poll = given()
                            .baseUri(SagaEnvironment.bookingBaseUrl())
                    .when()
                            .get("/bookings/" + sagaId)
                    .then()
                            .statusCode(200)
                            .extract().response();

                    String status = poll.jsonPath().getString("status");
                    lastStatus.set(status);
                    LOG.info("[E2E] Polling saga={} status={}", sagaId, status);

                    assertThat(status)
                            .as("Saga %s should reach a terminal state (last=%s)", sagaId, status)
                            .isIn("COMPLETED", "CANCELLED", "COMPENSATION_FAILED");
                });

        return given()
                .baseUri(SagaEnvironment.bookingBaseUrl())
        .when()
                .get("/bookings/" + sagaId)
        .then()
                .statusCode(200)
                .extract().response();
    }

    // ------------------------------------------------------------------
    // Health checks
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Booking service health endpoint returns UP")
    void bookingServiceIsHealthy() {
        given()
                .baseUri(SagaEnvironment.bookingBaseUrl())
        .when()
                .get("/actuator/health")
        .then()
                .statusCode(200);
    }

    // ------------------------------------------------------------------
    // Happy path - full saga completes
    // ------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("POST /bookings starts saga and returns 201 with IN_PROGRESS status")
    void startBookingSaga() {
        Response response = given()
                .baseUri(SagaEnvironment.bookingBaseUrl())
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerName": "E2E Test Customer",
                          "destination": "Paris",
                          "amount": 499.99
                        }
                        """)
        .when()
                .post("/bookings")
        .then()
                .statusCode(201)
                .extract().response();

        String sagaId = response.jsonPath().getString("sagaId");
        assertThat(sagaId).isNotBlank();
        assertThat(response.jsonPath().getString("status")).isEqualTo("IN_PROGRESS");
        assertThat(response.jsonPath().getString("customerName")).isEqualTo("E2E Test Customer");
        assertThat(response.jsonPath().getString("destination")).isEqualTo("Paris");
    }

    @Test
    @Order(3)
    @DisplayName("Saga eventually reaches COMPLETED status with all steps COMPLETED")
    void sagaCompletesSuccessfully() {
        // Start a new saga
        String sagaId = given()
                .baseUri(SagaEnvironment.bookingBaseUrl())
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerName": "E2E Happy Path",
                          "destination": "Tokyo",
                          "amount": 1200.00
                        }
                        """)
        .when()
                .post("/bookings")
        .then()
                .statusCode(201)
                .extract().jsonPath().getString("sagaId");

        Response finalState = awaitTerminalState(sagaId, 60);
        assertThat(finalState.jsonPath().getString("status")).isEqualTo("COMPLETED");

        List<Map<String, String>> steps = finalState.jsonPath().getList("steps");
        assertThat(steps).hasSize(3);
        assertThat(steps).extracting(s -> s.get("name"))
                .containsExactly("FLIGHT", "HOTEL", "PAYMENT");
        assertThat(steps).allSatisfy(step ->
                assertThat(step.get("status")).isEqualTo("RESERVED"));
    }

    // ------------------------------------------------------------------
    // Compensation path - flight failure (step 1 fails, no compensation needed)
    // ------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("BLOCKED customer triggers flight rejection - saga CANCELLED, no compensation steps")
    void flightRejectionCancelsSagaWithoutCompensation() {
        String sagaId = given()
                .baseUri(SagaEnvironment.bookingBaseUrl())
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerName": "BLOCKED Passenger",
                          "destination": "Paris",
                          "amount": 100.00
                        }
                        """)
        .when()
                .post("/bookings")
        .then()
                .statusCode(201)
                .extract().jsonPath().getString("sagaId");

        Response finalState = awaitTerminalState(sagaId, 90);
        assertThat(finalState.jsonPath().getString("status")).isEqualTo("CANCELLED");

        List<Map<String, String>> steps = finalState.jsonPath().getList("steps");
        assertThat(steps).hasSize(3);

        // Flight failed - no reservation was made, so nothing to compensate
        assertThat(steps.get(0).get("name")).isEqualTo("FLIGHT");
        assertThat(steps.get(0).get("status")).isEqualTo("FAILED");

        // Hotel and Payment were never attempted
        assertThat(steps.get(1).get("name")).isEqualTo("HOTEL");
        assertThat(steps.get(1).get("status")).isEqualTo("PENDING");

        assertThat(steps.get(2).get("name")).isEqualTo("PAYMENT");
        assertThat(steps.get(2).get("status")).isEqualTo("PENDING");
    }

    // ------------------------------------------------------------------
    // Compensation path - hotel failure (step 2 fails, flight compensated)
    // ------------------------------------------------------------------

    @Test
    @Order(11)
    @DisplayName("Mars destination triggers hotel rejection - saga CANCELLED, flight compensated")
    void hotelRejectionCompensatesFlight() {
        String sagaId = given()
                .baseUri(SagaEnvironment.bookingBaseUrl())
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerName": "E2E Compensation Test",
                          "destination": "Mars",
                          "amount": 500.00
                        }
                        """)
        .when()
                .post("/bookings")
        .then()
                .statusCode(201)
                .extract().jsonPath().getString("sagaId");

        Response finalState = awaitTerminalState(sagaId, 90);
        assertThat(finalState.jsonPath().getString("status")).isEqualTo("CANCELLED");

        List<Map<String, String>> steps = finalState.jsonPath().getList("steps");
        assertThat(steps).hasSize(3);

        // Flight was reserved, then compensated
        assertThat(steps.get(0).get("name")).isEqualTo("FLIGHT");
        assertThat(steps.get(0).get("status")).isEqualTo("COMPENSATED");

        // Hotel failed
        assertThat(steps.get(1).get("name")).isEqualTo("HOTEL");
        assertThat(steps.get(1).get("status")).isEqualTo("FAILED");

        // Payment was never attempted
        assertThat(steps.get(2).get("name")).isEqualTo("PAYMENT");
        assertThat(steps.get(2).get("status")).isEqualTo("PENDING");
    }

    // ------------------------------------------------------------------
    // Compensation path - payment failure (step 3 fails, hotel + flight compensated)
    // ------------------------------------------------------------------

    @Test
    @Order(12)
    @DisplayName("Amount >= 1M triggers payment rejection - saga CANCELLED, hotel and flight compensated")
    void paymentRejectionCompensatesHotelAndFlight() {
        String sagaId = given()
                .baseUri(SagaEnvironment.bookingBaseUrl())
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerName": "E2E Full Compensation",
                          "destination": "Tokyo",
                          "amount": 1000000.00
                        }
                        """)
        .when()
                .post("/bookings")
        .then()
                .statusCode(201)
                .extract().jsonPath().getString("sagaId");

        Response finalState = awaitTerminalState(sagaId, 90);
        assertThat(finalState.jsonPath().getString("status")).isEqualTo("CANCELLED");

        List<Map<String, String>> steps = finalState.jsonPath().getList("steps");
        assertThat(steps).hasSize(3);

        // Flight was reserved, then compensated
        assertThat(steps.get(0).get("name")).isEqualTo("FLIGHT");
        assertThat(steps.get(0).get("status")).isEqualTo("COMPENSATED");

        // Hotel was reserved, then compensated
        assertThat(steps.get(1).get("name")).isEqualTo("HOTEL");
        assertThat(steps.get(1).get("status")).isEqualTo("COMPENSATED");

        // Payment failed
        assertThat(steps.get(2).get("name")).isEqualTo("PAYMENT");
        assertThat(steps.get(2).get("status")).isEqualTo("FAILED");
    }

    // ------------------------------------------------------------------
    // GET /bookings - list all
    // ------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("GET /bookings returns a non-empty list")
    void listBookingsReturnsResults() {
        given()
                .baseUri(SagaEnvironment.bookingBaseUrl())
        .when()
                .get("/bookings")
        .then()
                .statusCode(200);

        List<?> bookings = given()
                .baseUri(SagaEnvironment.bookingBaseUrl())
        .when()
                .get("/bookings")
        .then()
                .extract().jsonPath().getList("content");

        assertThat(bookings).isNotEmpty();
    }

    // ------------------------------------------------------------------
    // Validation - bad request
    // ------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("POST /bookings with missing fields returns 400")
    void startBookingWithInvalidPayloadReturns400() {
        given()
                .baseUri(SagaEnvironment.bookingBaseUrl())
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerName": "",
                          "destination": "",
                          "amount": -10
                        }
                        """)
        .when()
                .post("/bookings")
        .then()
                .statusCode(400);
    }

    // ------------------------------------------------------------------
    // GET /bookings/{id} - not found
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("GET /bookings/{nonExistentId} returns 404")
    void getNonExistentBookingReturns404() {
        given()
                .baseUri(SagaEnvironment.bookingBaseUrl())
        .when()
                .get("/bookings/00000000-0000-0000-0000-000000000000")
        .then()
                .statusCode(404);
    }
}
