package com.github.arkadiusz97.discordmessagesllmmoderator.service.notifications;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;

public interface Notification {
    String name();
    void notify(Boolean removedMessage, PromptResponse promptResponse, String messageContent);
}
