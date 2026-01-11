package com.github.arkadiusz97.discordmessagesllmmoderator.service.notifications;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailNotification implements Notification {

    public void notify(boolean removedMessage, PromptResponse promptResponse) {
        log.debug("[TODO implement this] Result from LlmClient for EmailNotification: {}", promptResponse);
    }

}
