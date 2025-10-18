package com.github.arkadiusz97.discordmessagesllmmoderator.model;

public record QueueMessage(
        String messageContent,
        Long channelId,
        Long messageId
) { }
