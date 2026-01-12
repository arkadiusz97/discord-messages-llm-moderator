package com.github.arkadiusz97.discordmessagesllmmoderator.service.notifications;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationsServiceImpl implements NotificationsService {

    private final Set<Notification> notificationStrategies;
    private final Environment environment;

    public void notify(boolean removedMessage, PromptResponse promptResponse, String messageContent) {
        if (!promptResponse.breaksRules()) {
            return;
        }
        notificationStrategies.forEach(notification -> {
            Boolean executeStrategy = environment.getProperty(
                    "app.notifications.enabled." + notification.name(), Boolean.class
            );
            if (Boolean.TRUE.equals(executeStrategy)) {
                log.debug("Executing notification strategy: '{}' for prompt response: {}",
                        notification.name(), promptResponse);
                notification.notify(removedMessage, promptResponse, messageContent);
            } else {
                log.debug("Notification strategy '{}' won't be executed for prompt response: {}",
                        notification.name(), promptResponse);
            }
        });
    }

}
