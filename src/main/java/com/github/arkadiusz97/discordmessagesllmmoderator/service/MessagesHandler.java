package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.QueueMessage;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

public interface MessagesHandler {
    void handle(QueueMessage in, Message message, Channel channel) throws Exception;
}
