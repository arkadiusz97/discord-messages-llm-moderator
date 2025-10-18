package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.exception.DiscordMessagesLlmModeratorException;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptRequest;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.QueueMessage;
import com.rabbitmq.client.Channel;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@TestPropertySource(properties = {
        "app.queue-name=discord-messages-llm-moderator-queue",
})
@ExtendWith(MockitoExtension.class)
public class MessagesHandlerTest {

    @InjectMocks
    private MessagesHandler messagesHandler;

    @Mock
    private LlmClient llmClient;

    @Mock
    private GatewayDiscordClient gatewayDiscordClient;

    @BeforeEach
    public void init() {
        messagesHandler = new MessagesHandler(llmClient, gatewayDiscordClient);
    }

    @Test
    public void handleMessageSuccessfully_whenDoesNotBreakRules() throws Exception {
        var messageContent = "message";
        var in = new QueueMessage(messageContent, 1L, 2L);

        var deliveryTag = 3L;
        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(deliveryTag);
        var message = new Message(messageContent.getBytes(), messageProperties);

        var channel = mock(Channel.class);

        when(llmClient.sendPrompt(new PromptRequest(messageContent)))
                .thenReturn(new PromptResponse(false, null));

        messagesHandler.listen(in, message, channel);

        verify(channel, times(1)).basicAck(deliveryTag, false);
        verifyNoMoreInteractions(channel);

        verifyNoInteractions(gatewayDiscordClient);
    }

    @Test
    public void handleMessageSuccessfully_whenBreaksRules() throws Exception {
        var messageContent = "message";
        var channelId = 1L;
        var messageId = 2L;
        var in = new QueueMessage(messageContent, channelId, messageId);

        var deliveryTag = 3L;
        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(deliveryTag);
        var rabbitmqMessage = new Message(messageContent.getBytes(), messageProperties);

        var rabbitmqChannel = mock(Channel.class);

        var discordMessage = mock(discord4j.core.object.entity.Message.class);
        var messageChannel = mock(MessageChannel.class);

        when(messageChannel.getMessageById(Snowflake.of(messageId)))
                .thenReturn(Mono.just(discordMessage));
        when(llmClient.sendPrompt(new PromptRequest(messageContent)))
                .thenReturn(new PromptResponse(true, "reason"));
        when(gatewayDiscordClient.getChannelById(Snowflake.of(channelId)))
                .thenReturn(Mono.just(messageChannel));
        when(discordMessage.delete()).thenReturn(Mono.empty());

        messagesHandler.listen(in, rabbitmqMessage, rabbitmqChannel);

        verify(rabbitmqChannel, times(1)).basicAck(deliveryTag, false);
        verifyNoMoreInteractions(rabbitmqChannel);

        verify(gatewayDiscordClient, times(1)).getChannelById(Snowflake.of(channelId));
        verifyNoMoreInteractions(gatewayDiscordClient);

        verify(discordMessage, times(1)).delete();
        verifyNoMoreInteractions(discordMessage);
    }

    @Test
    public void negativeAcknowledge_whenLlmClientThrowsException() throws Exception {
        var messageContent = "message";
        var channelId = 1L;
        var messageId = 2L;
        var in = new QueueMessage(messageContent, channelId, messageId);

        var deliveryTag = 3L;
        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(deliveryTag);
        var rabbitmqMessage = new Message(messageContent.getBytes(), messageProperties);

        var rabbitmqChannel = mock(Channel.class);

        when(llmClient.sendPrompt(new PromptRequest(messageContent)))
                .thenThrow(RuntimeException.class);

        assertThrows(DiscordMessagesLlmModeratorException.class,
                () -> messagesHandler.listen(in, rabbitmqMessage, rabbitmqChannel)
        );

        verify(rabbitmqChannel, times(1)).basicNack(deliveryTag, false, true);
        verifyNoMoreInteractions(rabbitmqChannel);

        verifyNoInteractions(gatewayDiscordClient);
    }

    @Test
    public void throwsException_whenGatewayDiscordClientAndBasicNackThrowException() throws Exception {
        var messageContent = "message";
        var channelId = 1L;
        var messageId = 2L;
        var in = new QueueMessage(messageContent, channelId, messageId);

        var deliveryTag = 3L;
        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(deliveryTag);
        var rabbitmqMessage = new Message(messageContent.getBytes(), messageProperties);

        var rabbitmqChannel = mock(Channel.class);

        when(llmClient.sendPrompt(new PromptRequest(messageContent)))
                .thenReturn(new PromptResponse(true, "reason"));
        when(gatewayDiscordClient.getChannelById(Snowflake.of(channelId)))
                .thenReturn(Mono.error(new IOException()));
        doThrow(IOException.class).when(rabbitmqChannel).basicNack(deliveryTag, false, true);

        messagesHandler.listen(in, rabbitmqMessage, rabbitmqChannel);

        verify(rabbitmqChannel, times(1)).basicNack(deliveryTag, false, true);
        assertThrows(IOException.class,
                () -> rabbitmqChannel.basicNack(deliveryTag, false, true)
        );
        verifyNoMoreInteractions(rabbitmqChannel);

        verify(gatewayDiscordClient, times(1)).getChannelById(Snowflake.of(channelId));
        verifyNoMoreInteractions(gatewayDiscordClient);
    }

    @Test
    public void throwsException_whenGatewayDiscordClientAndBasicAckThrowException() throws Exception {
        var messageContent = "message";
        var channelId = 1L;
        var messageId = 2L;
        var in = new QueueMessage(messageContent, channelId, messageId);

        var deliveryTag = 3L;
        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(deliveryTag);
        var rabbitmqMessage = new Message(messageContent.getBytes(), messageProperties);

        var rabbitmqChannel = mock(Channel.class);

        var discordMessage = mock(discord4j.core.object.entity.Message.class);
        var messageChannel = mock(MessageChannel.class);

        when(messageChannel.getMessageById(Snowflake.of(messageId)))
                .thenReturn(Mono.just(discordMessage));
        when(llmClient.sendPrompt(new PromptRequest(messageContent)))
                .thenReturn(new PromptResponse(true, "reason"));
        when(gatewayDiscordClient.getChannelById(Snowflake.of(channelId)))
                .thenReturn(Mono.just(messageChannel));
        when(discordMessage.delete()).thenReturn(Mono.empty());
        doThrow(IOException.class).when(rabbitmqChannel).basicAck(deliveryTag, false);

        messagesHandler.listen(in, rabbitmqMessage, rabbitmqChannel);

        verify(rabbitmqChannel, times(1)).basicAck(deliveryTag, false);
        assertThrows(IOException.class,
                () -> rabbitmqChannel.basicAck(deliveryTag, false)
        );
        verifyNoMoreInteractions(rabbitmqChannel);

        verify(gatewayDiscordClient, times(1)).getChannelById(Snowflake.of(channelId));
        verifyNoMoreInteractions(gatewayDiscordClient);
    }

}
