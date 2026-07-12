package com.rzodeczko.contracts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rzodeczko.infrastructure.messaging.dto.ParticipantCommandMessage;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Weryfikacja kontraktu serializacji wiadomosci command i reply.
 *
 * ParticipantCommandMessage (booking-service → participant) musi byc zgodne
 * z FlightCommandMessageDto / HotelCommandMessage / PaymentCommandMessage
 * po stronie odbiorcy.
 *
 * SagaReplyMessage (participant → booking-service) musi byc zgodne
 * z SagaReplyMessageDto po stronie nadawcy.
 */
class CommandMessageContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final UUID SAGA_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    // ------------------------------------------------------------------
    // ParticipantCommandMessage — wysylane do uczestnikow sagi
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("ParticipantCommandMessage (orchestrator → participant)")
    class CommandContract {

        @Test
        void reserveCommand_shouldHaveExpectedShape() throws JsonProcessingException {
            ParticipantCommandMessage command = new ParticipantCommandMessage(
                    SAGA_ID, "RESERVE", "Jan Kowalski", "Mars", new BigDecimal("1500.00")
            );

            JsonNode json = objectMapper.valueToTree(command);

            assertThat(json.size()).isEqualTo(5);
            assertThat(json.get("sagaId").asText()).isEqualTo(SAGA_ID.toString());
            assertThat(json.get("action").asText()).isEqualTo("RESERVE");
            assertThat(json.get("customerName").asText()).isEqualTo("Jan Kowalski");
            assertThat(json.get("destination").asText()).isEqualTo("Mars");
            assertThat(json.get("amount").decimalValue()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        void cancelCommand_shouldHaveExpectedShape() throws JsonProcessingException {
            ParticipantCommandMessage command = new ParticipantCommandMessage(
                    SAGA_ID, "CANCEL", "Jan Kowalski", "Mars", new BigDecimal("1500.00")
            );

            JsonNode json = objectMapper.valueToTree(command);

            assertThat(json.get("action").asText()).isEqualTo("CANCEL");
        }

        @Test
        void commandShouldSurviveRoundTrip() throws JsonProcessingException {
            ParticipantCommandMessage original = new ParticipantCommandMessage(
                    SAGA_ID, "RESERVE", "Anna Nowak", "Europa", new BigDecimal("3200.50")
            );

            String json = objectMapper.writeValueAsString(original);
            ParticipantCommandMessage restored = objectMapper.readValue(json, ParticipantCommandMessage.class);

            assertThat(restored).isEqualTo(original);
        }

        @Test
        void commandFieldNames_shouldMatchParticipantExpectation() throws JsonProcessingException {
            // Weryfikacja ze nazwy pol sa zgodne z oczekiwaniami flight-service
            // (FlightCommandMessageDto: sagaId, action, customerName, destination, amount)
            ParticipantCommandMessage command = new ParticipantCommandMessage(
                    SAGA_ID, "RESERVE", "Test", "Moon", BigDecimal.ONE
            );

            JsonNode json = objectMapper.valueToTree(command);

            assertThat(json.fieldNames())
                    .toIterable()
                    .containsExactlyInAnyOrder("sagaId", "action", "customerName", "destination", "amount");
        }
    }

    // ------------------------------------------------------------------
    // SagaReplyMessage — odbierane od uczestnikow sagi
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("SagaReplyMessage (participant → orchestrator)")
    class ReplyContract {

        @Test
        void shouldDeserializeSuccessReply() throws JsonProcessingException {
            // JSON w formacie produkowanym przez uczestnika (SagaReplyMessageDto)
            String json = """
                    {
                      "sagaId": "550e8400-e29b-41d4-a716-446655440000",
                      "step": "FLIGHT",
                      "action": "RESERVE",
                      "status": "SUCCESS",
                      "reason": null
                    }
                    """;

            SagaReplyMessage reply = objectMapper.readValue(json, SagaReplyMessage.class);

            assertThat(reply.sagaId()).isEqualTo(SAGA_ID);
            assertThat(reply.step()).isEqualTo("FLIGHT");
            assertThat(reply.action()).isEqualTo("RESERVE");
            assertThat(reply.status()).isEqualTo("SUCCESS");
            assertThat(reply.reason()).isNull();
        }

        @Test
        void shouldDeserializeFailureReply() throws JsonProcessingException {
            String json = """
                    {
                      "sagaId": "550e8400-e29b-41d4-a716-446655440000",
                      "step": "HOTEL",
                      "action": "RESERVE",
                      "status": "FAILURE",
                      "reason": "No cabins available"
                    }
                    """;

            SagaReplyMessage reply = objectMapper.readValue(json, SagaReplyMessage.class);

            assertThat(reply.status()).isEqualTo("FAILURE");
            assertThat(reply.reason()).isEqualTo("No cabins available");
            assertThat(reply.step()).isEqualTo("HOTEL");
        }

        @Test
        void replyFieldNames_shouldMatchProducerFormat() throws JsonProcessingException {
            String json = """
                    {
                      "sagaId": "550e8400-e29b-41d4-a716-446655440000",
                      "step": "PAYMENT",
                      "action": "CANCEL",
                      "status": "SUCCESS",
                      "reason": null
                    }
                    """;

            JsonNode node = objectMapper.readTree(json);

            assertThat(node.fieldNames())
                    .toIterable()
                    .containsExactlyInAnyOrder("sagaId", "step", "action", "status", "reason");
        }

        @Test
        void replyShouldSurviveRoundTrip() throws JsonProcessingException {
            SagaReplyMessage original = new SagaReplyMessage(
                    SAGA_ID, "FLIGHT", "RESERVE", "SUCCESS", null
            );

            String json = objectMapper.writeValueAsString(original);
            SagaReplyMessage restored = objectMapper.readValue(json, SagaReplyMessage.class);

            assertThat(restored).isEqualTo(original);
        }
    }
}
