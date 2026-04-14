package com.github.arkadiusz97.discordmessagesllmmoderator.service.notifications;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GuildChannelNotification implements Notification {

    private final GatewayDiscordClient client;
    private final String channelName;

    public GuildChannelNotification(GatewayDiscordClient client,
                @Value("${app.notifications.channel-name}") String channelName) {
        this.client = client;
        this.channelName = channelName;
    }

    @Override
    public String name() {
        return "guild-channel";
    }

    @Override
    public void notify(Boolean removedMessage, PromptResponse promptResponse, String messageContent, Long userId,
            Long serverId) {
        String message = getMessageContent(removedMessage, promptResponse, messageContent, userId);
        createMessage(message, serverId)
                .doOnError(e -> log.error("Error during sending message '{}' to guild: {} {}", message,
                        serverId, e.getMessage(), e))
                .doOnSuccess(_ -> log.debug("Message '{}' sent to guild {}", message, serverId))
                .subscribe();
    }

    private Mono<Void> createMessage(String messageContentToSend, Long serverId) {
        return client.getGuildById(Snowflake.of(serverId))
                .flatMapMany(Guild::getChannels)
                .ofType(GuildChannel.class)
                .filter(channel -> channel.getName().equals(channelName))
                .next()
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Channel not found: {}", channelName);
                    return Mono.empty();
                }))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(messageContentToSend))
                .then();
    }

    private String getMessageContent(Boolean removedMessage, PromptResponse promptResponse, String messageContent,
            Long userId) {
        return "**Received message** " + messageContent +
                "\n**From user** <@" + userId + ">" +
                "\n**It breaks rule** " + promptResponse.reasonForBreakingRules() +
                "\nMessage will " +
                (removedMessage ? "be removed" : "not be removed");
    }

}
