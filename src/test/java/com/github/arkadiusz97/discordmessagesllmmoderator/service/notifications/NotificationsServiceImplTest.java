package com.github.arkadiusz97.discordmessagesllmmoderator.service.notifications;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationsServiceImplTest {

    @InjectMocks
    private NotificationsServiceImpl notificationsServiceImpl;

    @Mock
    private Environment environment;

    @ParameterizedTest
    @MethodSource("shouldExecuteAllStrategiesMarkedForExecutionData")
    void shouldExecuteAllStrategiesMarkedForExecution(Boolean breaksRules, Integer invokationsOfNotifications) {
        Notification notificationForExecution1 = mock(Notification.class);
        Notification notificationForExecution2 = mock(Notification.class);
        Notification notificationNotForExecution = mock(Notification.class);
        ReflectionTestUtils.setField(notificationsServiceImpl,"notificationStrategies",
                Set.of(notificationForExecution1, notificationForExecution2, notificationNotForExecution)
        );
        PromptResponse promptResponse = new PromptResponse(breaksRules, "reason");

        if (breaksRules) {
            when(environment.getProperty("app.notifications.enabled.notification1", Boolean.class))
                    .thenReturn(true);
            when(environment.getProperty("app.notifications.enabled.notification2", Boolean.class))
                    .thenReturn(true);
            when(environment.getProperty("app.notifications.enabled.notification3", Boolean.class))
                    .thenReturn(false);

            when(notificationForExecution1.name()).thenReturn("notification1");
            when(notificationForExecution2.name()).thenReturn("notification2");
            when(notificationNotForExecution.name()).thenReturn("notification3");
        }
        notificationsServiceImpl.notify(true, promptResponse, "message content");

        verify(notificationForExecution1, times(invokationsOfNotifications))
                .notify(anyBoolean(), any(PromptResponse.class), anyString());
        verify(notificationForExecution2, times(invokationsOfNotifications))
                .notify(anyBoolean(), any(PromptResponse.class), anyString());
        verify(notificationNotForExecution, times(0))
                .notify(anyBoolean(), any(PromptResponse.class), anyString());
    }

    private static Stream<Arguments> shouldExecuteAllStrategiesMarkedForExecutionData() {
        return Stream.of(
                Arguments.of(false, 0),
                Arguments.of(true, 1)
        );
    }

}
