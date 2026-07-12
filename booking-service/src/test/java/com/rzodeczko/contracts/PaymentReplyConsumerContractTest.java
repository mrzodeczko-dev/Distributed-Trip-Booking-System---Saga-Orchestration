package com.rzodeczko.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy konsumenckie — weryfikuja, ze booking-service poprawnie
 * deserializuje wiadomosci zdefiniowane w kontraktach payment-service.
 */
@SpringBootTest(
        classes = PaymentReplyConsumerContractTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureMessageVerifier
@AutoConfigureStubRunner(
        ids = "com.rzodeczko:payment-service:0.0.1:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class PaymentReplyConsumerContractTest {

    @Autowired
    private StubTrigger stubTrigger;

    @Autowired
    private ContractVerifierMessaging contractVerifierMessaging;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeSuccessReplyOnReserve() throws Exception {
        stubTrigger.trigger("payment_reserve_success");

        ContractVerifierMessage message = contractVerifierMessaging.receive("x.saga.replies");
        assertThat(message).isNotNull();

        SagaReplyMessage reply = objectMapper.readValue(
                message.getPayload().toString(), SagaReplyMessage.class);

        assertThat(reply.sagaId()).isNotNull();
        assertThat(reply.step()).isEqualTo("PAYMENT");
        assertThat(reply.action()).isEqualTo("RESERVE");
        assertThat(reply.status()).isEqualTo("SUCCESS");
        assertThat(reply.reason()).isNull();
    }

    @Test
    void shouldDeserializeFailureReplyOnReserve() throws Exception {
        stubTrigger.trigger("payment_reserve_failure");

        ContractVerifierMessage message = contractVerifierMessaging.receive("x.saga.replies");
        assertThat(message).isNotNull();

        SagaReplyMessage reply = objectMapper.readValue(
                message.getPayload().toString(), SagaReplyMessage.class);

        assertThat(reply.step()).isEqualTo("PAYMENT");
        assertThat(reply.action()).isEqualTo("RESERVE");
        assertThat(reply.status()).isEqualTo("FAILURE");
        assertThat(reply.reason()).isEqualTo("Credit limit exceeded");
    }

    @Test
    void shouldDeserializeSuccessReplyOnCancel() throws Exception {
        stubTrigger.trigger("payment_cancel_success");

        ContractVerifierMessage message = contractVerifierMessaging.receive("x.saga.replies");
        assertThat(message).isNotNull();

        SagaReplyMessage reply = objectMapper.readValue(
                message.getPayload().toString(), SagaReplyMessage.class);

        assertThat(reply.step()).isEqualTo("PAYMENT");
        assertThat(reply.action()).isEqualTo("CANCEL");
        assertThat(reply.status()).isEqualTo("SUCCESS");
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
