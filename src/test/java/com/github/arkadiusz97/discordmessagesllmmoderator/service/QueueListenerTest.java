package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.QueueMessage;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QueueListenerTest {

    @Mock
    private MessagesHandler messagesHandler;

    @Mock
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @InjectMocks
    private QueueListener queueListener;

    @Test
    void shouldHandleMessage() throws Exception {
        QueueMessage queueMessage = mock(QueueMessage.class);
        Message message = mock(Message.class);
        Channel channel = mock(Channel.class);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(threadPoolTaskExecutor).execute(any(Runnable.class));

        queueListener.listen(queueMessage, message, channel);

        verify(threadPoolTaskExecutor).execute(any(Runnable.class));
        verify(messagesHandler).handle(queueMessage, message, channel);
    }
}
