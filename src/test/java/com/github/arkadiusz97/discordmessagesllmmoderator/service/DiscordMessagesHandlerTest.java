package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.exception.DiscordMessagesLlmModeratorException;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptRequest;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;
import com.github.arkadiusz97.discordmessagesllmmoderator.model.QueueMessage;
import com.github.arkadiusz97.discordmessagesllmmoderator.service.llmclient.LlmClient;
import com.github.arkadiusz97.discordmessagesllmmoderator.service.notifications.NotificationsService;
import com.rabbitmq.client.Channel;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DiscordMessagesHandlerTest {

    @InjectMocks
    private DiscordMessagesHandler discordMessagesHandler;

    @Mock
    private LlmClient llmClient;

    @Mock
    private GatewayDiscordClient gatewayDiscordClient;

    @Mock
    private NotificationsService notificationsService;

    private static final String MESSAGE_CONTENT = "message";
    private static final Long CHANNEL_ID = 1L;
    private static final Long MESSAGE_ID = 2L;
    private static final Long DELIVERY_TAG = 3L;
    private static final QueueMessage IN = new QueueMessage(MESSAGE_CONTENT, CHANNEL_ID, MESSAGE_ID);

    @Test
    public void handleMessageSuccessfully_whenDoesNotBreakRules() throws Exception {
        ReflectionTestUtils.setField(discordMessagesHandler, "removeMessages", true);
        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(DELIVERY_TAG);
        var message = new Message(MESSAGE_CONTENT.getBytes(), messageProperties);

        var channel = mock(Channel.class);
        var promptResponse = new PromptResponse(false, null);
        when(llmClient.sendPrompt(new PromptRequest(MESSAGE_CONTENT)))
                .thenReturn(new PromptResponse(false, null));

        discordMessagesHandler.handle(IN, message, channel);

        verify(channel, times(1)).basicAck(DELIVERY_TAG, false);
        verifyNoMoreInteractions(channel);

        verifyNoInteractions(gatewayDiscordClient);
        verify(notificationsService, times(1)).notify(true, promptResponse,
                MESSAGE_CONTENT);
    }

    @ParameterizedTest
    @MethodSource("discordMessagesHandlerData")
    public void handleMessageSuccessfully_whenBreaksRules(boolean removeMessages,
            Integer discordMessageCalls) throws Exception {
        ReflectionTestUtils.setField(discordMessagesHandler, "removeMessages", removeMessages);

        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(DELIVERY_TAG);
        var rabbitmqMessage = new Message(MESSAGE_CONTENT.getBytes(), messageProperties);

        var rabbitmqChannel = mock(Channel.class);

        var discordMessage = mock(discord4j.core.object.entity.Message.class);
        var messageChannel = mock(MessageChannel.class);

        var promptResponse = new PromptResponse(true, "reason");
        when(llmClient.sendPrompt(new PromptRequest(MESSAGE_CONTENT)))
                .thenReturn(promptResponse);
        if (removeMessages) {
            when(gatewayDiscordClient.getChannelById(Snowflake.of(CHANNEL_ID)))
                    .thenReturn(Mono.just(messageChannel));
            when(messageChannel.getMessageById(Snowflake.of(MESSAGE_ID)))
                    .thenReturn(Mono.just(discordMessage));
            when(discordMessage.delete()).thenReturn(Mono.empty());
        }

        discordMessagesHandler.handle(IN, rabbitmqMessage, rabbitmqChannel);

        verify(rabbitmqChannel, times(1)).basicAck(DELIVERY_TAG, false);
        verifyNoMoreInteractions(rabbitmqChannel);

        verify(gatewayDiscordClient, times(discordMessageCalls)).getChannelById(Snowflake.of(CHANNEL_ID));
        verifyNoMoreInteractions(gatewayDiscordClient);

        verify(discordMessage, times(discordMessageCalls)).delete();
        verifyNoMoreInteractions(discordMessage);
        verify(notificationsService, times(1)).notify(removeMessages, promptResponse,
                MESSAGE_CONTENT);
    }

    @Test
    public void negativeAcknowledge_whenLlmClientThrowsException() throws Exception {
        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(DELIVERY_TAG);
        var rabbitmqMessage = new Message(MESSAGE_CONTENT.getBytes(), messageProperties);

        var rabbitmqChannel = mock(Channel.class);

        when(llmClient.sendPrompt(new PromptRequest(MESSAGE_CONTENT)))
                .thenThrow(RuntimeException.class);

        assertThrows(DiscordMessagesLlmModeratorException.class,
                () -> discordMessagesHandler.handle(IN, rabbitmqMessage, rabbitmqChannel)
        );

        verify(rabbitmqChannel, times(1)).basicNack(DELIVERY_TAG, false, true);
        verifyNoMoreInteractions(rabbitmqChannel);

        verifyNoInteractions(gatewayDiscordClient);
        verify(notificationsService, times(0)).notify(anyBoolean(), any(PromptResponse.class),
                anyString());
    }

    @Test
    public void throwsException_whenGatewayDiscordClientAndBasicNackThrowException() throws Exception {
        ReflectionTestUtils.setField(discordMessagesHandler, "removeMessages", true);

        var messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(DELIVERY_TAG);
        var rabbitmqMessage = new Message(MESSAGE_CONTENT.getBytes(), messageProperties);

        var rabbitmqChannel = mock(Channel.class);

        var promptResponse = new PromptResponse(true, "reason");
        when(llmClient.sendPrompt(new PromptRequest(MESSAGE_CONTENT)))
                .thenReturn(promptResponse);
        when(gatewayDiscordClient.getChannelById(Snowflake.of(CHANNEL_ID)))
                .thenReturn(Mono.error(new IOException()));
        doThrow(IOException.class).when(rabbitmqChannel).basicNack(DELIVERY_TAG, false, true);

        discordMessagesHandler.handle(IN, rabbitmqMessage, rabbitmqChannel);

        verify(rabbitmqChannel, times(1)).basicNack(DELIVERY_TAG, false, true);
        assertThrows(IOException.class,
                () -> rabbitmqChannel.basicNack(DELIVERY_TAG, false, true)
        );
        verifyNoMoreInteractions(rabbitmqChannel);

        verify(gatewayDiscordClient, times(1)).getChannelById(Snowflake.of(CHANNEL_ID));
        verifyNoMoreInteractions(gatewayDiscordClient);
        verify(notificationsService, times(0)).notify(anyBoolean(), any(PromptResponse.class),
                anyString());
    }

    @Test
    public void throwsException_whenGatewayDiscordClientAndBasicAckThrowException() throws Exception {
        ReflectionTestUtils.setField(discordMessagesHandler, "removeMessages", true);

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

        discordMessagesHandler.handle(IN, rabbitmqMessage, rabbitmqChannel);

        verify(rabbitmqChannel, times(1)).basicAck(DELIVERY_TAG, false);
        assertThrows(IOException.class,
                () -> rabbitmqChannel.basicAck(DELIVERY_TAG, false)
        );
        verifyNoMoreInteractions(rabbitmqChannel);

        verify(gatewayDiscordClient, times(1)).getChannelById(Snowflake.of(CHANNEL_ID));
        verifyNoMoreInteractions(gatewayDiscordClient);
        verify(notificationsService, times(0)).notify(anyBoolean(), any(PromptResponse.class),
                anyString());
    }

    private static Stream<Arguments> discordMessagesHandlerData() {
        return Stream.of(
                Arguments.of(true, 1),
                Arguments.of(false, 0)
        );
    }

}
