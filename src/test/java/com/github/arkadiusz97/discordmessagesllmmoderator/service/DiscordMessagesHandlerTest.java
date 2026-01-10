package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.exception.DiscordMessagesLlmModeratorException;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptRequest;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.QueueMessage;
import com.rabbitmq.client.Channel;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
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
public class DiscordMessagesHandlerTest {

    @InjectMocks
    private DiscordMessagesHandler discordMessagesHandler;

    @Mock
    private LlmClient llmClient;

    @Mock
    private GatewayDiscordClient gatewayDiscordClient;

    private static final String MESSAGE_CONTENT = "message";
    private static final Long CHANNEL_ID = 1L;
    private static final Long MESSAGE_ID = 2L;
    private static final Long DELIVERY_TAG = 3L;

    @Test
    public void handleMessageSuccessfully_whenDoesNotBreakRules() throws Exception {
        var in = new QueueMessage(MESSAGE_CONTENT, CHANNEL_ID, MESSAGE_ID);

        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(DELIVERY_TAG);
        var message = new Message(MESSAGE_CONTENT.getBytes(), messageProperties);

        var channel = mock(Channel.class);

        when(llmClient.sendPrompt(new PromptRequest(MESSAGE_CONTENT)))
                .thenReturn(new PromptResponse(false, null));

        discordMessagesHandler.handle(in, message, channel);

        verify(channel, times(1)).basicAck(DELIVERY_TAG, false);
        verifyNoMoreInteractions(channel);

        verifyNoInteractions(gatewayDiscordClient);
    }

    @Test
    public void handleMessageSuccessfully_whenBreaksRules() throws Exception {
        var in = new QueueMessage(MESSAGE_CONTENT, CHANNEL_ID, MESSAGE_ID);

        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(DELIVERY_TAG);
        var rabbitmqMessage = new Message(MESSAGE_CONTENT.getBytes(), messageProperties);

        var rabbitmqChannel = mock(Channel.class);

        var discordMessage = mock(discord4j.core.object.entity.Message.class);
        var messageChannel = mock(MessageChannel.class);

        when(messageChannel.getMessageById(Snowflake.of(MESSAGE_ID)))
                .thenReturn(Mono.just(discordMessage));
        when(llmClient.sendPrompt(new PromptRequest(MESSAGE_CONTENT)))
                .thenReturn(new PromptResponse(true, "reason"));
        when(gatewayDiscordClient.getChannelById(Snowflake.of(CHANNEL_ID)))
                .thenReturn(Mono.just(messageChannel));
        when(discordMessage.delete()).thenReturn(Mono.empty());

        discordMessagesHandler.handle(in, rabbitmqMessage, rabbitmqChannel);

        verify(rabbitmqChannel, times(1)).basicAck(DELIVERY_TAG, false);
        verifyNoMoreInteractions(rabbitmqChannel);

        verify(gatewayDiscordClient, times(1)).getChannelById(Snowflake.of(CHANNEL_ID));
        verifyNoMoreInteractions(gatewayDiscordClient);

        verify(discordMessage, times(1)).delete();
        verifyNoMoreInteractions(discordMessage);
    }

    @Test
    public void negativeAcknowledge_whenLlmClientThrowsException() throws Exception {
        var in = new QueueMessage(MESSAGE_CONTENT, CHANNEL_ID, MESSAGE_ID);

        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(DELIVERY_TAG);
        var rabbitmqMessage = new Message(MESSAGE_CONTENT.getBytes(), messageProperties);

        var rabbitmqChannel = mock(Channel.class);

        when(llmClient.sendPrompt(new PromptRequest(MESSAGE_CONTENT)))
                .thenThrow(RuntimeException.class);

        assertThrows(DiscordMessagesLlmModeratorException.class,
                () -> discordMessagesHandler.handle(in, rabbitmqMessage, rabbitmqChannel)
        );

        verify(rabbitmqChannel, times(1)).basicNack(DELIVERY_TAG, false, true);
        verifyNoMoreInteractions(rabbitmqChannel);

        verifyNoInteractions(gatewayDiscordClient);
    }

    @Test
    public void throwsException_whenGatewayDiscordClientAndBasicNackThrowException() throws Exception {
        var in = new QueueMessage(MESSAGE_CONTENT, CHANNEL_ID, MESSAGE_ID);

        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(DELIVERY_TAG);
        var rabbitmqMessage = new Message(MESSAGE_CONTENT.getBytes(), messageProperties);

        var rabbitmqChannel = mock(Channel.class);

        when(llmClient.sendPrompt(new PromptRequest(MESSAGE_CONTENT)))
                .thenReturn(new PromptResponse(true, "reason"));
        when(gatewayDiscordClient.getChannelById(Snowflake.of(CHANNEL_ID)))
                .thenReturn(Mono.error(new IOException()));
        doThrow(IOException.class).when(rabbitmqChannel).basicNack(DELIVERY_TAG, false, true);

        discordMessagesHandler.handle(in, rabbitmqMessage, rabbitmqChannel);

        verify(rabbitmqChannel, times(1)).basicNack(DELIVERY_TAG, false, true);
        assertThrows(IOException.class,
                () -> rabbitmqChannel.basicNack(DELIVERY_TAG, false, true)
        );
        verifyNoMoreInteractions(rabbitmqChannel);

        verify(gatewayDiscordClient, times(1)).getChannelById(Snowflake.of(CHANNEL_ID));
        verifyNoMoreInteractions(gatewayDiscordClient);
    }

    @Test
    public void throwsException_whenGatewayDiscordClientAndBasicAckThrowException() throws Exception {
        var in = new QueueMessage(MESSAGE_CONTENT, CHANNEL_ID, MESSAGE_ID);

        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(DELIVERY_TAG);
        var rabbitmqMessage = new Message(MESSAGE_CONTENT.getBytes(), messageProperties);

        var rabbitmqChannel = mock(Channel.class);

        var discordMessage = mock(discord4j.core.object.entity.Message.class);
        var messageChannel = mock(MessageChannel.class);

        when(messageChannel.getMessageById(Snowflake.of(MESSAGE_ID)))
                .thenReturn(Mono.just(discordMessage));
        when(llmClient.sendPrompt(new PromptRequest(MESSAGE_CONTENT)))
                .thenReturn(new PromptResponse(true, "reason"));
        when(gatewayDiscordClient.getChannelById(Snowflake.of(CHANNEL_ID)))
                .thenReturn(Mono.just(messageChannel));
        when(discordMessage.delete()).thenReturn(Mono.empty());
        doThrow(IOException.class).when(rabbitmqChannel).basicAck(DELIVERY_TAG, false);

        discordMessagesHandler.handle(in, rabbitmqMessage, rabbitmqChannel);

        verify(rabbitmqChannel, times(1)).basicAck(DELIVERY_TAG, false);
        assertThrows(IOException.class,
                () -> rabbitmqChannel.basicAck(DELIVERY_TAG, false)
        );
        verifyNoMoreInteractions(rabbitmqChannel);

        verify(gatewayDiscordClient, times(1)).getChannelById(Snowflake.of(CHANNEL_ID));
        verifyNoMoreInteractions(gatewayDiscordClient);
    }

}
