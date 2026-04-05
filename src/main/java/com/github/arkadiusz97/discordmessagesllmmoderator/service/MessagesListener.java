package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.QueueMessage;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
            var eventMessage = event.getMessage();
            long messageId = eventMessage.getId().asLong();
            if (eventMessage.getAuthor().isEmpty()) {
                log.debug("Message " + messageId + " is ignored, because author is not provided");
            } else if (eventMessage.getGuildId().isEmpty()) {
                log.debug("Message " + messageId + " is ignored, because guildId is not provided");
            } else {
                var queueMessage = new QueueMessage(
                        eventMessage.getContent(),
                        eventMessage.getChannelId().asLong(),
                        eventMessage.getId().asLong(),
                        eventMessage.getGuildId().get().asLong(),
                        eventMessage.getAuthor().get().getId().asLong()
                );
                RabbitTemplate.convertAndSend(queueName, queueMessage);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        if (client != null) {
            client.logout().block();
        }
    }

}
