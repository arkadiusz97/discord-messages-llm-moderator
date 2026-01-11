package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.exception.DiscordMessagesLlmModeratorException;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptRequest;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.QueueMessage;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.extern.slf4j.Slf4j;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Service
@Slf4j
public class DiscordMessagesHandler implements MessagesHandler {

    private final LlmClient llmClient;
    private final GatewayDiscordClient gatewayDiscordClient;
    private final Boolean removeMessages;

    public DiscordMessagesHandler(LlmClient llmClient, GatewayDiscordClient gatewayDiscordClient,
            @Value("${app.remove-messages}") Boolean removeMessages) {
        this.llmClient = llmClient;
        this.gatewayDiscordClient = gatewayDiscordClient;
        this.removeMessages = removeMessages;
    }

    public void handle(QueueMessage in, Message message, Channel channel) throws Exception {
        log.debug("Message content: {}", in.messageContent());
        log.debug("Message id: {}", in.messageId());
        log.debug("Channel id: {}", in.channelId());
        PromptResponse promptResponse;
        try {
            promptResponse = llmClient.sendPrompt(
                    new PromptRequest(in.messageContent())
            );
        } catch (Exception e) {
            negativeAcknowledge(message, channel);
            throw new DiscordMessagesLlmModeratorException("Error during sending request to llm", e);
        }
        log.debug("Result from LlmClient: {}", promptResponse);
        handleLlmResponse(in, message, channel, promptResponse);
    }

    private void handleLlmResponse(QueueMessage in, Message message, Channel channel, PromptResponse promptResponse)
            throws Exception {
        String messageContent = in.messageContent();
        Long messageId = in.messageId();
        Long channelId = in.channelId();
        Boolean breaksRules = promptResponse.breaksRules();

        if (breaksRules && removeMessages) {
            deleteMessage(channelId, messageId)
                    .doOnError(e -> {
                        log.error("Error deleting: {}", e.getMessage(), e);
                        try {
                            negativeAcknowledge(message, channel);
                        } catch (IOException e1) {
                            log.error("Error during negative acknowledge: {}", e1.getMessage(), e1);
                        }
                    })
                    .doOnSuccess(_ -> {
                        try {
                            acknowledge(message, channel);
                        } catch (IOException e) {
                            log.error("Error during successfull acknowledge: {}", e.getMessage(), e);
                        }
                        log.debug("Deleted successfully message with content: {}, message id: {}, Channel id: {}",
                                messageContent, messageId, channelId);
                    })
                    .subscribe();
        } else if (breaksRules) {
            log.debug("Message, which breaks rules is not deleted. Content: {}, message id: {}, Channel id: {}",
                    messageContent, messageId, channelId);
            acknowledge(message, channel);
        } else {
            acknowledge(message, channel);
        }
    }

    private void negativeAcknowledge(Message message, Channel channel) throws IOException {
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
    }

    private void acknowledge(Message message, Channel channel) throws IOException {
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    private Mono<Void> deleteMessage(Long channelId, Long messageId) {
        return gatewayDiscordClient.getChannelById(Snowflake.of(channelId))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.getMessageById(Snowflake.of(messageId)))
                .flatMap(discord4j.core.object.entity.Message::delete)
                .then();
    }

}
