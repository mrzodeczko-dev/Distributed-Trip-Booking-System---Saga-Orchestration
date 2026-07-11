package com.rzodeczko.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ParticipantTopologyProperties.class)
public class HotelTopologyConfig {
    private final ParticipantTopologyProperties topology;

    @Bean
    public Declarables hotelTopology() {
        DirectExchange commandsExchange = new DirectExchange(topology.commandsExchange(), true, false);
        DirectExchange repliesExchange = new DirectExchange(topology.repliesExchange(), true, false);
        DirectExchange dlxExchange = new DirectExchange(topology.dlxExchange(), true, false);

        Queue commandQueue = QueueBuilder
                .durable(topology.commandQueue())
                .deadLetterExchange(topology.dlxExchange())
                .deadLetterRoutingKey(topology.commandDlqRoutingKey())
                .build();

        Binding commandBinding = BindingBuilder
                .bind(commandQueue)
                .to(commandsExchange)
                .with(topology.commandRoutingKey());

        Queue commandDlq = QueueBuilder.durable(topology.commandDlq()).build();

        Binding commandDlqBinding = BindingBuilder
                .bind(commandDlq)
                .to(dlxExchange)
                .with(topology.commandDlqRoutingKey());

        return new Declarables(
                commandsExchange,
                repliesExchange,
                dlxExchange,
                commandQueue,
                commandBinding,
                commandDlq,
                commandDlqBinding
        );
    }
}
