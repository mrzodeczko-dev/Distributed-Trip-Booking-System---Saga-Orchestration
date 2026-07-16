package com.rzodeczko.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void objectMapperShouldRegisterJavaTimeModule() {
        ObjectMapper mapper = config.objectMapper();

        assertThat(mapper.getRegisteredModuleIds()).contains(new JavaTimeModule().getTypeId());
    }

    @Test
    void objectMapperShouldNotSerializeDatesAsTimestamps() {
        ObjectMapper mapper = config.objectMapper();

        assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }

    @Test
    void objectMapperShouldSerializeInstantAsIsoString() throws Exception {
        ObjectMapper mapper = config.objectMapper();
        String result = mapper.writeValueAsString(Instant.parse("2026-01-01T10:00:00Z"));

        assertThat(result).contains("2026-01-01T10:00:00Z");
    }

    @Test
    void jsonMessageConverterShouldReturnJacksonConverter() {
        MessageConverter converter = config.jsonMessageConverter();

        assertThat(converter).isInstanceOf(JacksonJsonMessageConverter.class);
    }
}
