package com.github.arkadiusz97.discordmessagesllmmoderator.service.notifications;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;

public interface Notification {
    void notify(boolean removedMessage, PromptResponse promptResponse);
}
