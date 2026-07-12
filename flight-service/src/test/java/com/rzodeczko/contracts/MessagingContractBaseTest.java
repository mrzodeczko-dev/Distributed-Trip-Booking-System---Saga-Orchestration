package com.rzodeczko.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.converter.YamlContract;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierReceiver;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierSender;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

        private final Map<String, Message<?>> sent = new ConcurrentHashMap<>();

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public MessageVerifierSender<Message<?>> messageVerifierSender() {
            return new MessageVerifierSender<>() {
                @Override
                public <T> void send(T payload, Map<String, Object> headers, String destination, @Nullable YamlContract contract) {
                    MessageBuilder<T> builder = MessageBuilder.withPayload(payload);
                    headers.forEach(builder::setHeader);
                    sent.put(destination, builder.build());
                }

                @Override
                public void send(Message<?> message, String destination, @Nullable YamlContract contract) {
                    sent.put(destination, message);
                }
            };
        }

        @Bean
        public MessageVerifierReceiver<Message<?>> messageVerifierReceiver() {
            return new MessageVerifierReceiver<>() {
                @Override
                public Message<?> receive(String destination, long timeout, TimeUnit timeUnit, @Nullable YamlContract contract) {
                    return sent.remove(destination);
                }

                @Override
                public Message<?> receive(String destination, YamlContract contract) {
                    return sent.remove(destination);
                }
            };
        }
    }
}
