package com.github.arkadiusz97.discordmessagesllmmoderator.service;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.QueueMessage;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MessagesListenerTest {

    @InjectMocks
    private MessagesListener messagesListener;

    @Mock
    private GatewayDiscordClient gatewayDiscordClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @ParameterizedTest
    @MethodSource("shouldSubscribeMessageCreateEventAndLogoutAtTheEnd")
    public void shouldSubscribeMessageCreateEventAndLogoutAtTheEnd(Boolean serverEmpty, Boolean userEmpty,
            Boolean isBot) {
        var queue = "test-queue";
        ReflectionTestUtils.setField(messagesListener, "queueName", queue);
        var messageContent = "message";
        var channelId = 1L;
        var messageId = 2L;
        var userId = 3L;
        var serverId = 4L;
        var user = mock(User.class);
        when(user.isBot()).thenReturn(isBot);
        when(user.getId()).thenReturn(Snowflake.of(userId));

        var message = mock(Message.class);
        when(message.getContent()).thenReturn(messageContent);
        when(message.getChannelId()).thenReturn(Snowflake.of(channelId));
        when(message.getId()).thenReturn(Snowflake.of(messageId));
        when(message.getAuthor()).thenReturn(userEmpty ? Optional.empty() : Optional.of(user));
        when(message.getGuildId()).thenReturn(serverEmpty ? Optional.empty() : Optional.of(Snowflake.of(serverId)));

        var event = mock(MessageCreateEvent.class);
        when(event.getMessage()).thenReturn(message);

        when(gatewayDiscordClient.on(MessageCreateEvent.class)).thenReturn(Flux.just(event));

        var logout = mock(Mono.class);
        when(gatewayDiscordClient.logout()).thenReturn(logout);

        messagesListener.init();
        messagesListener.shutdown();

        if (serverEmpty || userEmpty || isBot) {
            verify(rabbitTemplate, never()).convertAndSend(eq(queue), any(QueueMessage.class));
        } else {
            ArgumentCaptor<QueueMessage> captor = ArgumentCaptor.forClass(QueueMessage.class);
            verify(rabbitTemplate).convertAndSend(eq(queue), captor.capture());

            var sent = captor.getValue();
            assertThat(sent.messageContent()).isEqualTo(messageContent);
            assertThat(sent.channelId()).isEqualTo(channelId);
            assertThat(sent.messageId()).isEqualTo(messageId);
        }

        verify(logout, times(1)).block();
    }

    @Test
    public void shouldDoNothingWhenClientIsNullDuringShutdown() {
        var rabbitTemplate = mock(RabbitTemplate.class);
        var logout = mock(Mono.class);

        var messagesListener = new MessagesListener(rabbitTemplate, null, "test-queue");

        messagesListener.shutdown();

        verify(logout, never()).block();
    }

    private static Stream<Arguments> shouldSubscribeMessageCreateEventAndLogoutAtTheEnd() {
        return Stream.of(
                Arguments.of(true, true, false),
                Arguments.of(true, false, false),
                Arguments.of(false, true, false),
                Arguments.of(false, false, false),
                Arguments.of(false, false, true)
        );
    }

}
