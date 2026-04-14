package com.github.arkadiusz97.discordmessagesllmmoderator.service.notifications;

import com.github.arkadiusz97.discordmessagesllmmoderator.model.PromptResponse;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateMono;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GuildChannelNotificationTest {

    @Mock
    private GatewayDiscordClient client;

    @InjectMocks
    private GuildChannelNotification guildChannelNotification;

    @ParameterizedTest
    @MethodSource("shouldNotifyData")
    public void shouldNotify(Boolean removedMessage, String existingChannelName, Boolean sendMessage) {
        var channelName = "channel-name";
        var messageContent = "message content";
        var userId = 1L;
        var serverId = 2L;
        var promptResponse = new PromptResponse(true, "reason");
        ReflectionTestUtils.setField(guildChannelNotification,"channelName", channelName);

        var guild = mock(Guild.class);
        var channelMock = mock(TextChannel.class);
        when(channelMock.getName()).thenReturn(existingChannelName);
        if (sendMessage) {
            when(channelMock.createMessage(any(String.class))).thenReturn(mock(MessageCreateMono.class));
        }
        when(guild.getChannels()).thenReturn(Flux.just(channelMock));
        var monoGuild = Mono.just(guild);
        when(client.getGuildById(Snowflake.of(serverId)))
                .thenReturn(monoGuild);

        guildChannelNotification.notify(removedMessage, promptResponse, messageContent, userId, serverId);

        if (sendMessage) {
            var sentMessageCaptor = ArgumentCaptor.forClass(String.class);
            verify(channelMock).createMessage(sentMessageCaptor.capture());
            var sentMessage = sentMessageCaptor.getValue();
            var expectedMessage = "**Received message** " + messageContent +
                    "\n**From user** <@" + userId + ">" +
                    "\n**It breaks rule** " + promptResponse.reasonForBreakingRules() +
                    "\nMessage will " +
                    (removedMessage ? "be removed" : "not be removed");
            assertEquals(expectedMessage, sentMessage);
        } else {
            verify(channelMock, never()).createMessage(anyString());
        }
    }

    @Test
    public void shouldReturnProperName() {
        assertEquals("guild-channel", guildChannelNotification.name());
    }

    private static Stream<Arguments> shouldNotifyData() {
        return Stream.of(
                Arguments.of(false, "channel-name", true),
                Arguments.of(true, "invalid-channel-name", false)
        );
    }

}
