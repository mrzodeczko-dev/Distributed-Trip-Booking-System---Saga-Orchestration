package com.rzodeczko.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq.topology")
public record ParticipantTopologyProperties(
        String commandsExchange,
        String repliesExchange,
        String dlxExchange,
        String commandQueue,
        String commandDlq,
        String commandRoutingKey,
        String commandDlqRoutingKey,
        String replyRoutingKey
) { }
