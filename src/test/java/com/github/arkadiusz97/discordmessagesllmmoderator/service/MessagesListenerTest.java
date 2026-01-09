package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.QueueMessage;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessagesListenerTest {

    @InjectMocks
    private MessagesListener messagesListener;

    @Mock
    private GatewayDiscordClient gatewayDiscordClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    public void shouldSubscribeMessageCreateEventAndLogoutAtTheEnd() {
        var queue = "test-queue";
        ReflectionTestUtils.setField(messagesListener, "queueName", queue);
        var messageContent = "message";
        var channelId = 1L;
        var messageId = 2L;

        var message = mock(Message.class);
        when(message.getContent()).thenReturn(messageContent);
        when(message.getChannelId()).thenReturn(Snowflake.of(channelId));
        when(message.getId()).thenReturn(Snowflake.of(messageId));

        var event = mock(MessageCreateEvent.class);
        when(event.getMessage()).thenReturn(message);

        when(gatewayDiscordClient.on(MessageCreateEvent.class)).thenReturn(Flux.just(event));

        var logout = mock(Mono.class);
        when(gatewayDiscordClient.logout()).thenReturn(logout);

        messagesListener.init();
        messagesListener.shutdown();

        ArgumentCaptor<QueueMessage> captor = ArgumentCaptor.forClass(QueueMessage.class);
        verify(rabbitTemplate).convertAndSend(eq(queue), captor.capture());

        var sent = captor.getValue();
        assertThat(sent.messageContent()).isEqualTo(messageContent);
        assertThat(sent.channelId()).isEqualTo(channelId);
        assertThat(sent.messageId()).isEqualTo(messageId);

        verify(logout, times(1)).block();
    }

    @Test
    public void shouldDoNothingWhenClientIsNullDuringShutdown() throws Exception {
        var rabbitTemplate = mock(RabbitTemplate.class);
        var logout = mock(Mono.class);

        var messagesListener = new MessagesListener(rabbitTemplate, null, "test-queue");

        messagesListener.shutdown();

        verify(logout, never()).block();
    }

}
