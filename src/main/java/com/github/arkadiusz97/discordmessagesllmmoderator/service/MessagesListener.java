package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.QueueMessage;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

@Service
public class MessagesListener {

    private final GatewayDiscordClient client;
    private final RabbitTemplate RabbitTemplate;
    private final String queueName;

    public MessagesListener(RabbitTemplate rabbitTemplate, GatewayDiscordClient client,
                            @Value("${app.queue-name}") String queueName) {
        this.RabbitTemplate = rabbitTemplate;
        this.client = client;
        this.queueName = queueName;
    }

    @PostConstruct
    public void init() {
        client.on(MessageCreateEvent.class).subscribe(event -> {
            var queueMessage = new QueueMessage(
                    event.getMessage().getContent(),
                    event.getMessage().getChannelId().asLong(),
                    event.getMessage().getId().asLong()
            );
            RabbitTemplate.convertAndSend(queueName, queueMessage);
        });
    }

    @PreDestroy
    public void shutdown() {
        if (client != null) {
            client.logout().block();
        }
    }

}
