package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.QueueMessage;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueListener {

    private final MessagesHandler messagesHandler;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @RabbitListener(queues = "${app.queue-name}")
    public void listen(QueueMessage in, Message message, Channel channel) throws Exception {
        threadPoolTaskExecutor.execute(
                () -> {
                    try {
                        messagesHandler.handle(in, message, channel);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

}
