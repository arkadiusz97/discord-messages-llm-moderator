package com.github.arkadiusz97.discordmessagesllmmoderator.service.notifications;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationsServiceImpl implements NotificationsService {

    private final Set<Notification> notificationStrategies;

    public void notify(boolean removedMessage, PromptResponse promptResponse) {
        notificationStrategies.forEach(notification -> notification.notify(removedMessage, promptResponse));
    }

}
