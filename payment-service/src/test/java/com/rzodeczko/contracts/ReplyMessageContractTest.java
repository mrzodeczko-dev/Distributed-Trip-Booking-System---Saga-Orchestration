package com.rzodeczko.contracts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReplyMessageContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final UUID SAGA_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Nested
    @DisplayName("Serialization (producer side)")
    class Serialization {

        @Test
        void successReply_shouldSerializeWithExpectedFields() throws JsonProcessingException {
            SagaReplyMessage reply = new SagaReplyMessage(
                    SAGA_ID, "PAYMENT", "RESERVE", "SUCCESS", null
            );

            JsonNode json = objectMapper.valueToTree(reply);

            assertThat(json.has("sagaId")).isTrue();
            assertThat(json.has("step")).isTrue();
            assertThat(json.has("action")).isTrue();
            assertThat(json.has("status")).isTrue();
            assertThat(json.has("reason")).isTrue();

            assertThat(json.get("sagaId").asText()).isEqualTo(SAGA_ID.toString());
            assertThat(json.get("step").asText()).isEqualTo("PAYMENT");
            assertThat(json.get("action").asText()).isEqualTo("RESERVE");
            assertThat(json.get("status").asText()).isEqualTo("SUCCESS");
            assertThat(json.get("reason").isNull()).isTrue();
        }

        @Test
        void failureReply_shouldSerializeReasonField() throws JsonProcessingException {
            SagaReplyMessage reply = new SagaReplyMessage(
                    SAGA_ID, "PAYMENT", "RESERVE", "FAILURE", "Credit limit exceeded"
            );

            JsonNode json = objectMapper.valueToTree(reply);

            assertThat(json.get("status").asText()).isEqualTo("FAILURE");
            assertThat(json.get("reason").asText()).isEqualTo("Credit limit exceeded");
        }

        @Test
        void cancelReply_shouldSerializeCorrectAction() throws JsonProcessingException {
            SagaReplyMessage reply = new SagaReplyMessage(
                    SAGA_ID, "PAYMENT", "CANCEL", "SUCCESS", null
            );

            JsonNode json = objectMapper.valueToTree(reply);

            assertThat(json.get("action").asText()).isEqualTo("CANCEL");
            assertThat(json.get("step").asText()).isEqualTo("PAYMENT");
        }

        @Test
        void serializedJson_shouldHaveExactlyFiveFields() throws JsonProcessingException {
            SagaReplyMessage reply = new SagaReplyMessage(
                    SAGA_ID, "PAYMENT", "RESERVE", "SUCCESS", null
            );

            JsonNode json = objectMapper.valueToTree(reply);

            assertThat(json.size()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Deserialization (consumer side)")
    class Deserialization {

        @Test
        void shouldDeserializeSuccessReply() throws JsonProcessingException {
            String json = """
                    {
                      "sagaId": "550e8400-e29b-41d4-a716-446655440000",
                      "step": "PAYMENT",
                      "action": "RESERVE",
                      "status": "SUCCESS",
                      "reason": null
                    }
                    """;

            SagaReplyMessage reply = objectMapper.readValue(json, SagaReplyMessage.class);

            assertThat(reply.sagaId()).isEqualTo(SAGA_ID);
            assertThat(reply.step()).isEqualTo("PAYMENT");
            assertThat(reply.action()).isEqualTo("RESERVE");
            assertThat(reply.status()).isEqualTo("SUCCESS");
            assertThat(reply.reason()).isNull();
        }

        @Test
        void shouldDeserializeFailureReply() throws JsonProcessingException {
            String json = """
                    {
                      "sagaId": "550e8400-e29b-41d4-a716-446655440000",
                      "step": "PAYMENT",
                      "action": "RESERVE",
                      "status": "FAILURE",
                      "reason": "Credit limit exceeded"
                    }
                    """;

            SagaReplyMessage reply = objectMapper.readValue(json, SagaReplyMessage.class);

            assertThat(reply.status()).isEqualTo("FAILURE");
            assertThat(reply.reason()).isEqualTo("Credit limit exceeded");
        }

        @Test
        void shouldSurviveRoundTrip() throws JsonProcessingException {
            SagaReplyMessage original = new SagaReplyMessage(
                    SAGA_ID, "PAYMENT", "CANCEL", "SUCCESS", null
            );

            String json = objectMapper.writeValueAsString(original);
            SagaReplyMessage restored = objectMapper.readValue(json, SagaReplyMessage.class);

            assertThat(restored).isEqualTo(original);
        }
    }
}
