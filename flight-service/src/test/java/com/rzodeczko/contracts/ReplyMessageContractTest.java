package com.rzodeczko.contracts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Weryfikacja kontraktu serializacji wiadomosci reply.
 * <p>
 * Te testy zapewniaja, ze SagaReplyMessageDto (publikowane przez flight-service)
 * jest zgodne z SagaReplyMessage (odbierane przez booking-service).
 * <p>
 * Jesli zmienisz nazwy pol w DTO, te testy wykryja zlamanie kontraktu
 * zanim wiadomosc trafi na produkcje.
 */
class ReplyMessageContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final UUID SAGA_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    // ------------------------------------------------------------------
    // Serialization: flight-service → JSON
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Serialization (producer side)")
    class Serialization {

        @Test
        void successReply_shouldSerializeWithExpectedFields() throws JsonProcessingException {
            SagaReplyMessageDto reply = new SagaReplyMessageDto(
                    SAGA_ID, "FLIGHT", "RESERVE", "SUCCESS", null
            );

            JsonNode json = objectMapper.valueToTree(reply);

            assertThat(json.has("sagaId")).isTrue();
            assertThat(json.has("step")).isTrue();
            assertThat(json.has("action")).isTrue();
            assertThat(json.has("status")).isTrue();
            assertThat(json.has("reason")).isTrue();

            assertThat(json.get("sagaId").asText()).isEqualTo(SAGA_ID.toString());
            assertThat(json.get("step").asText()).isEqualTo("FLIGHT");
            assertThat(json.get("action").asText()).isEqualTo("RESERVE");
            assertThat(json.get("status").asText()).isEqualTo("SUCCESS");
            assertThat(json.get("reason").isNull()).isTrue();
        }

        @Test
        void failureReply_shouldSerializeReasonField() throws JsonProcessingException {
            SagaReplyMessageDto reply = new SagaReplyMessageDto(
                    SAGA_ID, "FLIGHT", "RESERVE", "FAILURE", "No seats available"
            );

            JsonNode json = objectMapper.valueToTree(reply);

            assertThat(json.get("status").asText()).isEqualTo("FAILURE");
            assertThat(json.get("reason").asText()).isEqualTo("No seats available");
        }

        @Test
        void cancelReply_shouldSerializeCorrectAction() throws JsonProcessingException {
            SagaReplyMessageDto reply = new SagaReplyMessageDto(
                    SAGA_ID, "FLIGHT", "CANCEL", "SUCCESS", null
            );

            JsonNode json = objectMapper.valueToTree(reply);

            assertThat(json.get("action").asText()).isEqualTo("CANCEL");
            assertThat(json.get("step").asText()).isEqualTo("FLIGHT");
        }

        @Test
        void serializedJson_shouldHaveExactlyFiveFields() throws JsonProcessingException {
            SagaReplyMessageDto reply = new SagaReplyMessageDto(
                    SAGA_ID, "FLIGHT", "RESERVE", "SUCCESS", null
            );

            JsonNode json = objectMapper.valueToTree(reply);

            assertThat(json.size()).isEqualTo(5);
        }
    }

    // ------------------------------------------------------------------
    // Deserialization: JSON → booking-service consumer
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Deserialization (consumer side)")
    class Deserialization {

        @Test
        void shouldDeserializeSuccessReply() throws JsonProcessingException {
            String json = """
                    {
                      "sagaId": "550e8400-e29b-41d4-a716-446655440000",
                      "step": "FLIGHT",
                      "action": "RESERVE",
                      "status": "SUCCESS",
                      "reason": null
                    }
                    """;

            SagaReplyMessageDto reply = objectMapper.readValue(json, SagaReplyMessageDto.class);

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
                      "step": "FLIGHT",
                      "action": "RESERVE",
                      "status": "FAILURE",
                      "reason": "No seats available"
                    }
                    """;

            SagaReplyMessageDto reply = objectMapper.readValue(json, SagaReplyMessageDto.class);

            assertThat(reply.status()).isEqualTo("FAILURE");
            assertThat(reply.reason()).isEqualTo("No seats available");
        }

        @Test
        void shouldSurviveRoundTrip() throws JsonProcessingException {
            SagaReplyMessageDto original = new SagaReplyMessageDto(
                    SAGA_ID, "FLIGHT", "CANCEL", "SUCCESS", null
            );

            String json = objectMapper.writeValueAsString(original);
            SagaReplyMessageDto restored = objectMapper.readValue(json, SagaReplyMessageDto.class);

            assertThat(restored).isEqualTo(original);
        }
    }
}
