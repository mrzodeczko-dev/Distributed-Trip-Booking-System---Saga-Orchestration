package com.rzodeczko.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bazowa klasa dla testow kontraktowych messaging.
 * <p>
 * Kontrakty Groovy definiuja ksztalt wiadomosci (outputMessage),
 * a ta klasa dostarcza mechanizm ich wyslania i weryfikacji.
 * <p>
 * Metody trigger_*() sa wywolywane przez wygenerowane testy kontraktowe —
 * nazwa metody odpowiada labelowi z kontraktu Groovy.
 */
@SpringBootTest(
        classes = MessagingContractBaseTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureMessageVerifier
public abstract class MessagingContractBaseTest {

    @Autowired
    private ContractVerifierMessaging contractVerifierMessaging;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID SAGA_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    /**
     * Trigger dla kontraktu: "flight_reserve_success"
     */
    public void flight_reserve_success() throws Exception {
        SagaReplyMessageDto reply = new SagaReplyMessageDto(
                SAGA_ID, "FLIGHT", "RESERVE", "SUCCESS", null
        );
        sendMessage(reply, "x.saga.replies");
    }

    /**
     * Trigger dla kontraktu: "flight_reserve_failure"
     */
    public void flight_reserve_failure() throws Exception {
        SagaReplyMessageDto reply = new SagaReplyMessageDto(
                SAGA_ID, "FLIGHT", "RESERVE", "FAILURE",
                "No rocket seats available - passenger on watchlist"
        );
        sendMessage(reply, "x.saga.replies");
    }

    /**
     * Trigger dla kontraktu: "flight_cancel_success"
     */
    public void flight_cancel_success() throws Exception {
        SagaReplyMessageDto reply = new SagaReplyMessageDto(
                SAGA_ID, "FLIGHT", "CANCEL", "SUCCESS", null
        );
        sendMessage(reply, "x.saga.replies");
    }

    private void sendMessage(Object payload, String destination) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        Map<String, Object> headers = new HashMap<>();
        headers.put("contentType", "application/json");
        ContractVerifierMessage message = new ContractVerifierMessage(json, headers);
        contractVerifierMessaging.send(message, destination);
    }

    @Configuration
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
