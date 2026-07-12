package com.rzodeczko.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy konsumenckie — weryfikują, że booking-service poprawnie
 * deserializuje wiadomości zdefiniowane w kontraktach flight-service.
 *
 * Stuby pochodzą z lokalnego repozytorium Maven
 * (flight-service musi być wcześniej zainstalowany: mvn install).
 */
@SpringBootTest(
        classes = FlightReplyConsumerContractTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureMessageVerifier
@AutoConfigureStubRunner(
        ids = "com.rzodeczko:flight-service:0.0.1:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class FlightReplyConsumerContractTest {

    @Autowired
    private StubTrigger stubTrigger;

    @Autowired
    private ContractVerifierMessaging contractVerifierMessaging;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void drainQueue() {
        // Opróżnij kolejkę in-memory, żeby testy były niezależne od siebie
        while (contractVerifierMessaging.receive("x.saga.replies") != null) {
            // drain
        }
    }

    @Test
    void shouldDeserializeSuccessReplyOnReserve() throws Exception {
        stubTrigger.trigger("flight_reserve_success");

        ContractVerifierMessage message = contractVerifierMessaging.receive("x.saga.replies");
        assertThat(message).isNotNull();

        SagaReplyMessage reply = objectMapper.readValue(
                message.getPayload().toString(), SagaReplyMessage.class);

        assertThat(reply.sagaId()).isNotNull();
        assertThat(reply.step()).isEqualTo("FLIGHT");
        assertThat(reply.action()).isEqualTo("RESERVE");
        assertThat(reply.status()).isEqualTo("SUCCESS");
        assertThat(reply.reason()).isNull();
    }

    @Test
    void shouldDeserializeFailureReplyOnReserve() throws Exception {
        stubTrigger.trigger("flight_reserve_failure");

        ContractVerifierMessage message = contractVerifierMessaging.receive("x.saga.replies");
        assertThat(message).isNotNull();

        SagaReplyMessage reply = objectMapper.readValue(
                message.getPayload().toString(), SagaReplyMessage.class);

        assertThat(reply.step()).isEqualTo("FLIGHT");
        assertThat(reply.action()).isEqualTo("RESERVE");
        assertThat(reply.status()).isEqualTo("FAILURE");
        assertThat(reply.reason()).isNotBlank();
    }

    @Test
    void shouldDeserializeSuccessReplyOnCancel() throws Exception {
        stubTrigger.trigger("flight_cancel_success");

        ContractVerifierMessage message = contractVerifierMessaging.receive("x.saga.replies");
        assertThat(message).isNotNull();

        SagaReplyMessage reply = objectMapper.readValue(
                message.getPayload().toString(), SagaReplyMessage.class);

        assertThat(reply.step()).isEqualTo("FLIGHT");
        assertThat(reply.action()).isEqualTo("CANCEL");
        assertThat(reply.status()).isEqualTo("SUCCESS");
    }

    @Configuration
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
