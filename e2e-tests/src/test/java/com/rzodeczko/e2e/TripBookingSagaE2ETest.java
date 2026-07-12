package com.rzodeczko.e2e;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @BeforeAll
    static void startEnvironment() {
        SagaEnvironment.start();
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

        // Poll until saga completes (or times out)
        await().atMost(60, TimeUnit.SECONDS)
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
                    assertThat(status)
                            .as("Saga %s should reach a terminal state", sagaId)
                            .isIn("COMPLETED", "CANCELLED", "COMPENSATION_FAILED");
                });

        // Final verification
        Response finalState = given()
                .baseUri(SagaEnvironment.bookingBaseUrl())
        .when()
                .get("/bookings/" + sagaId)
        .then()
                .statusCode(200)
                .extract().response();

        assertThat(finalState.jsonPath().getString("status")).isEqualTo("COMPLETED");

        List<Map<String, String>> steps = finalState.jsonPath().getList("steps");
        assertThat(steps).hasSize(3);
        assertThat(steps).extracting(s -> s.get("name"))
                .containsExactly("FLIGHT", "HOTEL", "PAYMENT");
        assertThat(steps).allSatisfy(step ->
                assertThat(step.get("status")).isEqualTo("COMPLETED"));
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
                .extract().jsonPath().getList("$");

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
