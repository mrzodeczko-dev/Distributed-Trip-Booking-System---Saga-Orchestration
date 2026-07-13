package com.rzodeczko;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessage;
import com.rzodeczko.infrastructure.outbox.OutboxEvent;
import com.rzodeczko.infrastructure.outbox.OutboxEventRepository;
import com.rzodeczko.infrastructure.persistence.entity.SagaInstanceEntity;
import com.rzodeczko.infrastructure.persistence.repository.JpaSagaInstanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test for the full saga flow:
 * POST booking → outbox saves command → publisher sends to RabbitMQ →
 * simulate reply → saga progresses → repeat for all steps.
 */
@AutoConfigureMockMvc
class SagaFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JpaSagaInstanceRepository jpaSagaInstanceRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void shouldStartSagaAndPersistToDatabase() throws Exception {
        MvcResult result = mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Jan Kowalski",
                                  "destination": "Bali",
                                  "amount": 4999.99
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sagaId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.customerName").value("Jan Kowalski"))
                .andReturn();

        String sagaId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("sagaId").asText();

        // Saga persisted in DB
        SagaInstanceEntity entity = jpaSagaInstanceRepository.findById(UUID.fromString(sagaId))
                .orElseThrow();
        assertThat(entity.getCustomerName()).isEqualTo("Jan Kowalski");
        assertThat(entity.getDestination()).isEqualTo("Bali");
        assertThat(entity.getSteps()).hasSize(3);

        // Outbox event created for first step (FLIGHT_RESERVE)
        List<OutboxEvent> events = outboxEventRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc();
        assertThat(events).anyMatch(e -> e.getEventType().equals("FLIGHT_RESERVE"));
    }

    @Test
    void shouldGetBookingById() throws Exception {
        // Start a saga
        MvcResult result = mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Anna Nowak",
                                  "destination": "Tokyo",
                                  "amount": 7500.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String sagaId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("sagaId").asText();

        // GET by ID
        mockMvc.perform(get("/bookings/" + sagaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sagaId").value(sagaId))
                .andExpect(jsonPath("$.customerName").value("Anna Nowak"));
    }

    @Test
    void shouldListAllBookings() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "List Test",
                                  "destination": "Paris",
                                  "amount": 1000.00
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturn404ForNonExistentSaga() throws Exception {
        mockMvc.perform(get("/bookings/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "",
                                  "destination": "",
                                  "amount": -1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPublishOutboxEventsToRabbitMq() throws Exception {
        // Start a saga — creates FLIGHT_RESERVE outbox event
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Outbox Test",
                                  "destination": "Rome",
                                  "amount": 3000.00
                                }
                                """))
                .andExpect(status().isCreated());

        // Wait for scheduled OutboxEventPublisher to pick up and publish
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<OutboxEvent> unpublished = outboxEventRepository
                    .findTop100ByPublishedFalseAndDeadLetteredFalseOrderByCreatedAtAsc();
            // All events for this saga should be published
            boolean allPublished = outboxEventRepository.findAll().stream()
                    .filter(e -> e.getEventType().equals("FLIGHT_RESERVE"))
                    .allMatch(OutboxEvent::isPublished);
            assertThat(allPublished).isTrue();
        });
    }

    @Test
    void shouldCompleteSagaOnAllSuccessReplies() throws Exception {
        // Start saga
        MvcResult result = mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Full Flow",
                                  "destination": "Hawaii",
                                  "amount": 9999.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String sagaId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("sagaId").asText();
        UUID sagaUuid = UUID.fromString(sagaId);

        // Simulate FLIGHT RESERVE SUCCESS reply via RabbitMQ
        sendReply(sagaUuid, "FLIGHT", "RESERVE", "SUCCESS", null);

        // Wait for saga to process and create HOTEL_RESERVE outbox event
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            SagaInstanceEntity saga = jpaSagaInstanceRepository.findById(sagaUuid).orElseThrow();
            assertThat(saga.getSteps().stream()
                    .filter(s -> s.getName().name().equals("FLIGHT"))
                    .findFirst().orElseThrow().getStatus().name())
                    .isEqualTo("RESERVED");
        });

        // Simulate HOTEL RESERVE SUCCESS reply
        sendReply(sagaUuid, "HOTEL", "RESERVE", "SUCCESS", null);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            SagaInstanceEntity saga = jpaSagaInstanceRepository.findById(sagaUuid).orElseThrow();
            assertThat(saga.getSteps().stream()
                    .filter(s -> s.getName().name().equals("HOTEL"))
                    .findFirst().orElseThrow().getStatus().name())
                    .isEqualTo("RESERVED");
        });

        // Simulate PAYMENT RESERVE SUCCESS reply
        sendReply(sagaUuid, "PAYMENT", "RESERVE", "SUCCESS", null);

        // Saga should complete
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            SagaInstanceEntity saga = jpaSagaInstanceRepository.findById(sagaUuid).orElseThrow();
            assertThat(saga.getStatus().name()).isEqualTo("COMPLETED");
        });
    }

    @Test
    void shouldCompensateOnFailure() throws Exception {
        MvcResult result = mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Compensation Test",
                                  "destination": "Mars",
                                  "amount": 50000.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String sagaId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("sagaId").asText();
        UUID sagaUuid = UUID.fromString(sagaId);

        // FLIGHT succeeds
        sendReply(sagaUuid, "FLIGHT", "RESERVE", "SUCCESS", null);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            SagaInstanceEntity saga = jpaSagaInstanceRepository.findById(sagaUuid).orElseThrow();
            assertThat(saga.getSteps().stream()
                    .filter(s -> s.getName().name().equals("FLIGHT"))
                    .findFirst().orElseThrow().getStatus().name())
                    .isEqualTo("RESERVED");
        });

        // HOTEL fails -> compensation starts
        sendReply(sagaUuid, "HOTEL", "RESERVE", "FAILURE", "No rooms available");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            SagaInstanceEntity saga = jpaSagaInstanceRepository.findById(sagaUuid).orElseThrow();
            assertThat(saga.getStatus().name()).isEqualTo("COMPENSATING");
        });

        // FLIGHT cancel succeeds -> saga cancelled
        sendReply(sagaUuid, "FLIGHT", "CANCEL", "SUCCESS", null);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            SagaInstanceEntity saga = jpaSagaInstanceRepository.findById(sagaUuid).orElseThrow();
            assertThat(saga.getStatus().name()).isEqualTo("CANCELLED");
        });
    }

    @Test
    void shouldHandleCompensationFailure() throws Exception {
        MvcResult result = mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "CompFail Test",
                                  "destination": "Moon",
                                  "amount": 99999.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String sagaId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("sagaId").asText();
        UUID sagaUuid = UUID.fromString(sagaId);

        // FLIGHT succeeds
        sendReply(sagaUuid, "FLIGHT", "RESERVE", "SUCCESS", null);
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            SagaInstanceEntity saga = jpaSagaInstanceRepository.findById(sagaUuid).orElseThrow();
            assertThat(saga.getSteps().stream()
                    .filter(s -> s.getName().name().equals("FLIGHT"))
                    .findFirst().orElseThrow().getStatus().name())
                    .isEqualTo("RESERVED");
        });

        // HOTEL fails
        sendReply(sagaUuid, "HOTEL", "RESERVE", "FAILURE", "Overbooked");
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            SagaInstanceEntity saga = jpaSagaInstanceRepository.findById(sagaUuid).orElseThrow();
            assertThat(saga.getStatus().name()).isEqualTo("COMPENSATING");
        });

        // FLIGHT cancel also fails
        sendReply(sagaUuid, "FLIGHT", "CANCEL", "FAILURE", "Cannot cancel");
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            SagaInstanceEntity saga = jpaSagaInstanceRepository.findById(sagaUuid).orElseThrow();
            assertThat(saga.getStatus().name()).isEqualTo("COMPENSATION_FAILED");
        });
    }

    private void sendReply(UUID sagaId, String step, String action, String status, String reason) {
        SagaReplyMessage reply = new SagaReplyMessage(sagaId, step, action, status, reason);
        rabbitTemplate.convertAndSend("x.saga.replies", "saga.reply", reply);
    }
}
