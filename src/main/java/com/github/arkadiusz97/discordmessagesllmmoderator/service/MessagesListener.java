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
            var messageId = eventMessage.getId().asLong();
            var author = eventMessage.getAuthor();
            var messagePrefix = "Message " + messageId + " is ignored, because ";
            if (author.isEmpty()) {
                log.debug(messagePrefix + "author is not provided");
            } else if (eventMessage.getGuildId().isEmpty()) {
                log.debug(messagePrefix + "guildId is not provided");
            } else if (author.get().isBot()) {
                log.debug(messagePrefix + "it was sent by bot");
            } else {
                var queueMessage = new QueueMessage(
                        eventMessage.getContent(),
                        eventMessage.getChannelId().asLong(),
                        eventMessage.getId().asLong(),
                        eventMessage.getGuildId().get().asLong(),
                        author.get().getId().asLong()
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
